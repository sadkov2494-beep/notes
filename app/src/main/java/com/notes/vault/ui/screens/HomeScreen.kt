package com.notes.vault.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteSection
import com.notes.vault.ui.components.AnimatedFingerprintIcon
import com.notes.vault.ui.components.BiometricState
import com.notes.vault.ui.components.NoteCard
import com.notes.vault.ui.components.PinPad
import com.notes.vault.ui.components.SectionToggle
import com.notes.vault.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class InlineEditorState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenGroup: (Long, Boolean) -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGroupDialog by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<InlineEditorState?>(null) }
    val narrow = LocalConfiguration.current.screenWidthDp < 700

    if (showGroupDialog) {
        CreateGroupDialog(
            isVault = false,
            onDismiss = { showGroupDialog = false },
            onCreate = { name, color, icon ->
                viewModel.createGroup(name, color, icon, isVault = false)
                showGroupDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заметки с Сейфом") },
                actions = {
                    IconButton(onClick = { showGroupDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Группа")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.section == NoteSection.ALL_NOTES && editor == null) {
                FloatingActionButton(onClick = {
                    editor = InlineEditorState()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SectionToggle(
                selectedVault = uiState.section == NoteSection.VAULT,
                onSelectNotes = { viewModel.setSection(NoteSection.ALL_NOTES) },
                onSelectVault = {
                    viewModel.setSection(NoteSection.VAULT)
                    onOpenVault()
                }
            )

            if (uiState.section == NoteSection.ALL_NOTES) {
                val currentEditor = editor
                if (narrow && currentEditor != null) {
                    // На узком экране редактор занимает всю область меню
                    InlineNoteEditorPane(
                        state = currentEditor,
                        onStateChange = { editor = it },
                        onSave = {
                            viewModel.saveQuickNote(
                                currentEditor.title,
                                currentEditor.content,
                                currentEditor.noteId
                            )
                            editor = null
                        },
                        onClose = { editor = null }
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(if (currentEditor != null) 0.45f else 1f)
                                .fillMaxHeight()
                        ) {
                            NotesListPane(
                                notes = uiState.notes,
                                groups = uiState.groups,
                                selectedNoteId = currentEditor?.noteId,
                                onOpenGroup = { onOpenGroup(it, false) },
                                onOpenNote = { note ->
                                    editor = InlineEditorState(
                                        noteId = note.id,
                                        title = note.title,
                                        content = note.content
                                    )
                                }
                            )
                        }

                        if (currentEditor != null) {
                            VerticalDivider()
                            Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                                InlineNoteEditorPane(
                                    state = currentEditor,
                                    onStateChange = { editor = it },
                                    onSave = {
                                        viewModel.saveQuickNote(
                                            currentEditor.title,
                                            currentEditor.content,
                                            currentEditor.noteId
                                        )
                                        editor = null
                                    },
                                    onClose = { editor = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesListPane(
    notes: List<NoteEntity>,
    groups: List<com.notes.vault.data.model.NoteGroupEntity>,
    selectedNoteId: Long?,
    onOpenGroup: (Long) -> Unit,
    onOpenNote: (NoteEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groups.isNotEmpty()) {
            item {
                Text(
                    "Группы",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(groups, key = { "g-${it.id}" }) { group ->
                NoteCard(
                    title = group.name,
                    subtitle = "Группа",
                    onClick = { onOpenGroup(group.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                Text(
                    "Все заметки",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        if (notes.isEmpty()) {
            item {
                Text(
                    "Нет заметок — нажмите + справа",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            items(notes, key = { it.id }) { note ->
                val selected = note.id == selectedNoteId
                NoteCard(
                    title = note.title,
                    subtitle = note.content,
                    onClick = { onOpenNote(note) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .then(
                            if (selected) Modifier.padding(start = 4.dp) else Modifier
                        )
                )
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun InlineNoteEditorPane(
    state: InlineEditorState,
    onStateChange: (InlineEditorState) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.noteId != null) "Заметка" else "Новая заметка",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = "Сохранить")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.title,
            onValueChange = { onStateChange(state.copy(title = it)) },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.content,
            onValueChange = { onStateChange(state.copy(content = it)) },
            label = { Text("Текст") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            minLines = 8
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сохранить")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockScreen(
    onUnlocked: () -> Unit,
    onSetup: () -> Unit,
    viewModel: com.notes.vault.ui.viewmodel.VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val error by viewModel.unlockError.collectAsStateWithLifecycle()
    var biometricState by remember { mutableStateOf(BiometricState.IDLE) }
    var pin by remember { mutableStateOf("") }
    var biometricTried by remember { mutableStateOf(false) }
    var showLegacyPassword by remember { mutableStateOf(false) }
    var legacyPassword by remember { mutableStateOf("") }

    if (!viewModel.isCreated) {
        LaunchedEffect(Unit) { onSetup() }
        return
    }

    val showBiometric = viewModel.biometricAvailable && viewModel.biometricKeyValid

    fun tryUnlock(code: String) {
        if (viewModel.unlockWithPin(code)) {
            onUnlocked()
        } else {
            pin = ""
            val vibrator = context.getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        scope.launch {
            biometricState = BiometricState.IDLE
            val success = viewModel.unlockWithBiometric(activity)
            biometricState = if (success) BiometricState.SUCCESS else BiometricState.ERROR
            if (success) {
                delay(150)
                onUnlocked()
            }
        }
    }

    LaunchedEffect(showBiometric) {
        if (showBiometric && !biometricTried) {
            biometricTried = true
            delay(300)
            launchBiometric()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сейф") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showBiometric) {
                AnimatedFingerprintIcon(state = biometricState)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Быстрый вход ${viewModel.biometricHint()}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { launchBiometric() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Text(
                        "Повторить биометрию",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("или введите PIN", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Введите PIN-код", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            PinPad(
                pin = pin,
                pinLength = 4,
                onPinChange = {
                    pin = it
                    viewModel.clearError()
                },
                onComplete = { tryUnlock(it) },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            TextButton(onClick = { showLegacyPassword = !showLegacyPassword }) {
                Text(if (showLegacyPassword) "Скрыть пароль" else "Старый мастер-пароль")
            }
            if (showLegacyPassword) {
                OutlinedTextField(
                    value = legacyPassword,
                    onValueChange = { legacyPassword = it },
                    label = { Text("Мастер-пароль") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        if (viewModel.unlockWithPassword(legacyPassword)) onUnlocked()
                        else {
                            val vibrator = context.getSystemService(Vibrator::class.java)
                            vibrator?.vibrate(
                                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Разблокировать") }
            }
        }
    }
}

@Composable
fun VaultSetupScreen(
    onComplete: () -> Unit,
    viewModel: com.notes.vault.ui.viewmodel.VaultViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Настройка Сейфа", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        when (step) {
            0 -> {
                Text("Придумайте PIN из 4 цифр", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                PinPad(
                    pin = pin,
                    onPinChange = { pin = it; error = null },
                    onComplete = { pin = it; step = 1 },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
            1 -> {
                Text("Повторите PIN", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                PinPad(
                    pin = confirmPin,
                    onPinChange = { confirmPin = it; error = null },
                    onComplete = {
                        if (it != pin) {
                            error = "PIN-коды не совпадают"
                            confirmPin = ""
                        } else {
                            step = 2
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
            else -> {
                Text(
                    "Фраза восстановления (мин. 5 символов) — на случай сброса PIN",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recovery,
                    onValueChange = { recovery = it },
                    label = { Text("Ключевая фраза") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        val err = viewModel.createVault(pin, pin, recovery)
                        if (err == null) {
                            viewModel.enableBiometric()
                            onComplete()
                        } else {
                            error = err
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recovery.length >= 5
                ) {
                    Text("Создать Сейф")
                }
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        if (step > 0) {
            TextButton(onClick = {
                step -= 1
                error = null
            }) { Text("Назад") }
        }
    }
}
