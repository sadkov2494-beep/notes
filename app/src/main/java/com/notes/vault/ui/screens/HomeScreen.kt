package com.notes.vault.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var draftingNew by remember { mutableStateOf(false) }

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
            if (uiState.section == NoteSection.ALL_NOTES) {
                FloatingActionButton(onClick = { draftingNew = true }) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.groups.isNotEmpty()) {
                        item {
                            Text(
                                "Группы",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(uiState.groups, key = { "g-${it.id}" }) { group ->
                            NoteCard(
                                title = group.name,
                                subtitle = "Группа",
                                onClick = { onOpenGroup(group.id, false) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            Text(
                                "Заметки",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (draftingNew) {
                        item(key = "draft") {
                            InlineNoteRow(
                                noteId = null,
                                initialTitle = "",
                                initialContent = "",
                                onCommit = { id, title, content ->
                                    viewModel.upsertNote(id, title, content) {
                                        draftingNew = false
                                    }
                                },
                                onDelete = { draftingNew = false }
                            )
                        }
                    }

                    items(uiState.notes, key = { it.id }) { note ->
                        InlineNoteRow(
                            noteId = note.id,
                            initialTitle = note.title,
                            initialContent = note.content,
                            onCommit = { id, title, content ->
                                viewModel.upsertNote(id, title, content)
                            },
                            onDelete = { noteId ->
                                if (noteId != null) viewModel.deleteNote(noteId)
                            }
                        )
                    }

                    if (uiState.notes.isEmpty() && !draftingNew) {
                        item {
                            Text(
                                "Нажмите + и пишите прямо здесь",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InlineNoteRow(
    noteId: Long?,
    initialTitle: String,
    initialContent: String,
    onCommit: (noteId: Long?, title: String, content: String) -> Unit,
    onDelete: (noteId: Long?) -> Unit
) {
    var title by remember(noteId, initialTitle) { mutableStateOf(initialTitle) }
    var content by remember(noteId, initialContent) { mutableStateOf(initialContent) }
    val scope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(450)
            if (title.isNotBlank() || content.isNotBlank()) {
                onCommit(noteId, title, content)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        scheduleSave()
                    },
                    placeholder = { Text("Заголовок") },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focus ->
                            if (!focus.isFocused) scheduleSave()
                        },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(onClick = {
                    saveJob?.cancel()
                    onDelete(noteId)
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    scheduleSave()
                },
                placeholder = { Text("Текст заметки…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused) scheduleSave()
                    },
                minLines = 2,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
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
    var password by remember { mutableStateOf("") }
    var biometricTried by remember { mutableStateOf(false) }

    if (!viewModel.isCreated) {
        LaunchedEffect(Unit) { onSetup() }
        return
    }

    val usePin = viewModel.usesPin
    val showBiometric = viewModel.biometricAvailable && viewModel.biometricKeyValid

    fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun tryPassword(value: String) {
        if (viewModel.unlockWithPassword(value)) onUnlocked()
        else {
            pin = ""
            vibrate()
        }
    }

    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        scope.launch {
            biometricState = BiometricState.IDLE
            val success = viewModel.unlockWithBiometric(activity)
            biometricState = if (success) BiometricState.SUCCESS else BiometricState.ERROR
            if (success) {
                delay(120)
                onUnlocked()
            }
        }
    }

    LaunchedEffect(showBiometric) {
        if (showBiometric && !biometricTried) {
            biometricTried = true
            delay(250)
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
                    Text("Повторить биометрию", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (usePin) {
                Text("Введите PIN-код", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                PinPad(
                    pin = pin,
                    pinLength = 4,
                    onPinChange = {
                        pin = it
                        viewModel.clearError()
                    },
                    onComplete = { tryPassword(it) },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            } else {
                Text("Введите мастер-пароль", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearError()
                    },
                    label = { Text("Мастер-пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Button(
                    onClick = { tryPassword(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotBlank()
                ) {
                    Text("Разблокировать")
                }
                Text(
                    "Сейф был создан с паролем. PIN появится после пересоздания Сейфа.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
