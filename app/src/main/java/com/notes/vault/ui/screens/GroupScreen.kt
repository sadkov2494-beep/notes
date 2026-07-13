package com.notes.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.VaultEntryEntity
import com.notes.vault.ui.components.NoteCard
import com.notes.vault.ui.viewmodel.GroupDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    groupId: Long,
    isVault: Boolean,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Группа") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isVault) {
            val entries by viewModel.observeVaultEntries(groupId).collectAsStateWithLifecycle(emptyList())
            VaultGroupList(entries, padding, onOpenNote)
        } else {
            val notes by viewModel.observeNotes(groupId).collectAsStateWithLifecycle(emptyList())
            NotesGroupList(notes, padding, onOpenNote)
        }
    }
}

@Composable
private fun NotesGroupList(
    notes: List<NoteEntity>,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onOpenNote: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                title = note.title,
                subtitle = note.content,
                onClick = { onOpenNote(note.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun VaultGroupList(
    entries: List<VaultEntryEntity>,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onOpenNote: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            NoteCard(
                title = entry.service,
                subtitle = entry.login,
                onClick = { onOpenNote(entry.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
