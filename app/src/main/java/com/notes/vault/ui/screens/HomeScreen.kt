package com.notes.vault.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.vault.data.model.NoteSection
import com.notes.vault.ui.components.NoteCard
import com.notes.vault.ui.components.SectionToggle
import com.notes.vault.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNote: (Long) -> Unit,
    onOpenGroup: (Long, Boolean) -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateNote: () -> Unit,
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
                FloatingActionButton(onClick = onCreateNote) {
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
                if (uiState.groups.isNotEmpty()) {
                    Text(
                        "Группы",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.groups, key = { it.id }) { group ->
                            NoteCard(
                                title = group.name,
                                subtitle = "Группа",
                                onClick = { onOpenGroup(group.id, false) },
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
                        items(uiState.notes.filter { it.groupId == null }, key = { it.id }) { note ->
                            NoteCard(
                                title = note.title,
                                subtitle = note.content,
                                onClick = { onOpenNote(note.id) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else if (uiState.notes.isEmpty()) {
                    Text(
                        "Нет заметок",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteCard(
                                title = note.title,
                                subtitle = note.content,
                                onClick = { onOpenNote(note.id) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
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
    val error by viewModel.unlockError.collectAsStateWithLifecycle()

    if (!viewModel.isCreated) {
        LaunchedEffect(Unit) { onSetup() }
        return
    }

    var password = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сейф") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Введите мастер-пароль", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Мастер-пароль") },
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = {
                    if (viewModel.unlockWithPassword(password.value)) {
                        onUnlocked()
                    } else {
                        val vibrator = context.getSystemService(Vibrator::class.java)
                        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Разблокировать")
            }
        }
    }
}

@Composable
fun VaultSetupScreen(
    onComplete: () -> Unit,
    viewModel: com.notes.vault.ui.viewmodel.VaultViewModel = hiltViewModel()
) {
    var password = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var confirm = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var recovery = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var error = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Настройка Сейфа", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Задайте мастер-пароль и фразу восстановления (мин. 5 символов)",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Мастер-пароль") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirm.value,
            onValueChange = { confirm.value = it },
            label = { Text("Подтвердите пароль") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = recovery.value,
            onValueChange = { recovery.value = it },
            label = { Text("Ключевая фраза восстановления") },
            modifier = Modifier.fillMaxWidth()
        )
        error.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Button(
            onClick = {
                val err = viewModel.createVault(password.value, confirm.value, recovery.value)
                if (err == null) {
                    viewModel.enableBiometric()
                    onComplete()
                } else {
                    error.value = err
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Создать Сейф")
        }
    }
}
