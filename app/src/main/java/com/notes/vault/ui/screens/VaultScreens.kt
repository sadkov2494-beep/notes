package com.notes.vault.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.vault.ui.components.NoteCard
import com.notes.vault.ui.viewmodel.VaultEntryViewModel
import com.notes.vault.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onCreateEntry: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сейф") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.lock()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.lock() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Заблокировать")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateEntry) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Text(
                "Нет записей",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    NoteCard(
                        title = entry.service,
                        subtitle = entry.login,
                        onClick = { onOpenEntry(entry.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEntryEditorScreen(
    entryId: Long,
    onBack: () -> Unit,
    viewModel: VaultEntryViewModel = hiltViewModel()
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    var service by remember(entry) { mutableStateOf(entry?.service ?: "") }
    var login by remember(entry) { mutableStateOf(entry?.login ?: "") }
    var password by remember(entry) { mutableStateOf(entry?.password ?: "") }
    var url by remember(entry) { mutableStateOf(entry?.url ?: "") }
    var comment by remember(entry) { mutableStateOf(entry?.comment ?: "") }
    var genLength by remember { mutableStateOf(16) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId > 0) "Запись" else "Новая запись") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.save(service, login, password, url, comment)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(value = service, onValueChange = { service = it }, label = { Text("Сервис") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.material3.Button(
                onClick = { password = viewModel.generatePassword(genLength, true, true, true) },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Сгенерировать пароль") }
            androidx.compose.material3.TextButton(onClick = { viewModel.copyPassword(password) }) {
                Text("Копировать пароль")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Ссылка") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Примечание") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    viewModel: com.notes.vault.ui.viewmodel.SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Тема")
            com.notes.vault.data.model.ThemeMode.entries.forEach { mode ->
                androidx.compose.material3.FilterChip(
                    selected = themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                    label = {
                        Text(
                            when (mode) {
                                com.notes.vault.data.model.ThemeMode.AUTO -> "Авто"
                                com.notes.vault.data.model.ThemeMode.LIGHT -> "Светлая"
                                com.notes.vault.data.model.ThemeMode.DARK -> "Тёмная"
                            }
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Экспорт (.nbk)")
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Импорт (.nbk)")
            }
        }
    }
}
