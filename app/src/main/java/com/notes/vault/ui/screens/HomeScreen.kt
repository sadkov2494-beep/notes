package com.notes.vault.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.vault.data.model.NoteGroupEntity
import com.notes.vault.data.model.NoteSection
import com.notes.vault.ui.components.AnimatedFingerprintIcon
import com.notes.vault.ui.components.BiometricState
import com.notes.vault.ui.components.GraphicalNoteCard
import com.notes.vault.ui.components.PinPad
import com.notes.vault.ui.components.SectionToggle
import com.notes.vault.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGroupDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Default.History, contentDescription = "История удалений")
                    }
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
                FloatingActionButton(onClick = { viewModel.addNoteAtTop() }) {
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
                GroupColumnsBar(
                    groups = uiState.groups,
                    selectedGroupId = uiState.selectedGroupId,
                    onSelect = { viewModel.selectGroup(it) }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        val accent = groupAccent(uiState.groups, note.groupId)
                        GraphicalNoteCard(
                            note = note,
                            accent = accent,
                            onDescriptionChange = { viewModel.updateDescription(note.id, it) },
                            onContentChange = { viewModel.updateContent(note.id, it) },
                            onSoftDelete = { viewModel.softDeleteNote(note.id) },
                            onHardDelete = { viewModel.hardDeleteNote(note.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (uiState.notes.isEmpty()) {
                        item {
                            Text(
                                "＋  — новая заметка появится сверху",
                                modifier = Modifier.padding(24.dp),
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
private fun GroupColumnsBar(
    groups: List<NoteGroupEntity>,
    selectedGroupId: Long?,
    onSelect: (Long?) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupColumnChip(
            label = "Все",
            color = MaterialTheme.colorScheme.primary,
            selected = selectedGroupId == null,
            onClick = { onSelect(null) }
        )
        GroupColumnChip(
            label = "◦◦◦",
            color = MaterialTheme.colorScheme.outline,
            selected = selectedGroupId != null && selectedGroupId < 0,
            onClick = { onSelect(-1L) }
        )
        groups.forEach { group ->
            GroupColumnChip(
                label = group.name.take(12),
                color = Color(group.colorArgb),
                selected = selectedGroupId == group.id,
                onClick = { onSelect(group.id) }
            )
        }
    }
}

@Composable
private fun GroupColumnChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(2.dp, color, RoundedCornerShape(14.dp))
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun groupAccent(groups: List<NoteGroupEntity>, groupId: Long?): Color {
    val found = groups.firstOrNull { it.id == groupId }
    return if (found != null) Color(found.colorArgb) else Color(0xFF8B5E3C)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedNotesScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val deleted by viewModel.deletedNotes.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История удалений") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (deleted.isEmpty()) {
                item {
                    Text(
                        "Пусто — свайп влево убирает заметку сюда",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(deleted, key = { it.id }) { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.3f)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        GraphicalMiniBars(note.content)
                    }
                    TextButton(onClick = { viewModel.restoreNote(note.id) }) {
                        Text("Вернуть")
                    }
                    TextButton(onClick = { viewModel.hardDeleteNote(note.id) }) {
                        Text("✕")
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphicalMiniBars(content: String) {
    val accent = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val lengths = listOf(0.8f, 0.55f, 0.65f).mapIndexed { i, base ->
            val factor = if (content.isBlank()) base else ((content.hashCode() + i * 13).mod(40) + 40) / 100f
            factor
        }
        lengths.forEach { w ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(w)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.5f))
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
                    "Фраза восстановления (мин. 5 символов)",
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
