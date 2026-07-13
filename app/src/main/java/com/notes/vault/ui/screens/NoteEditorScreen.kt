package com.notes.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.notes.vault.data.model.ChecklistItem
import com.notes.vault.ui.components.ChecklistItemRow
import com.notes.vault.ui.viewmodel.NoteEditorViewModel
import com.notes.vault.util.MarkdownHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()

    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }
    var isChecklist by remember(note) { mutableStateOf(note?.isChecklist == true) }
    val checklistItems = remember(note) {
        mutableStateListOf<ChecklistItem>().apply {
            note?.checklistJson?.let { json ->
                addAll(MarkdownHelper.parseChecklist(json))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId > 0) "Редактирование" else "Новая заметка") },
                navigationIcon = {
                    IconButton(onClick = {
                        val checklistJson = if (isChecklist) viewModel.encodeChecklist(checklistItems) else null
                        viewModel.save(title, content, checklistJson, note?.reminderAt)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onPickImage) {
                        Icon(Icons.Default.Image, contentDescription = "Фото")
                    }
                    IconButton(onClick = { isChecklist = !isChecklist }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Чек-лист")
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { content = MarkdownHelper.wrapBold(content) }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Жирный")
                }
                IconButton(onClick = { content = MarkdownHelper.wrapItalic(content) }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Курсив")
                }
                TextButton(onClick = { content = MarkdownHelper.prefixHeading(content, 1) }) { Text("H1") }
                TextButton(onClick = { content = MarkdownHelper.prefixHeading(content, 2) }) { Text("H2") }
                TextButton(onClick = { content = MarkdownHelper.prefixHeading(content, 3) }) { Text("H3") }
                TextButton(onClick = { content = MarkdownHelper.prefixBullet(content) }) { Text("•") }
                TextButton(onClick = { content = MarkdownHelper.prefixNumbered(content) }) { Text("1.") }
            }
            if (isChecklist) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(checklistItems, key = { it.id }) { item ->
                        ChecklistItemRow(
                            item = item,
                            onToggle = {
                                val idx = checklistItems.indexOfFirst { it.id == item.id }
                                if (idx >= 0) checklistItems[idx] = item.copy(checked = !item.checked)
                            },
                            onTextChange = {}
                        )
                    }
                    item {
                        TextButton(onClick = {
                            checklistItems.add(viewModel.createChecklistItem("Новый пункт"))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Добавить пункт")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Текст заметки") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    minLines = 8
                )
            }
            if (attachments.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attachments, key = { it.id }) { attachment ->
                        AsyncImage(
                            model = attachment.filePath,
                            contentDescription = null,
                            modifier = Modifier.height(80.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
