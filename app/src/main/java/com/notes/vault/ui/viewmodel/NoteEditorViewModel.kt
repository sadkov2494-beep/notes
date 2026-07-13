package com.notes.vault.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notes.vault.data.model.AttachmentEntity
import com.notes.vault.data.model.ChecklistItem
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.repository.AttachmentStorage
import com.notes.vault.data.repository.NotesRepository
import com.notes.vault.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
    private val attachmentStorage: AttachmentStorage,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val initialNoteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentNoteId = MutableStateFlow(initialNoteId)
    val currentNoteId = _currentNoteId.asStateFlow()

    val note = _currentNoteId.flatMapLatest { id ->
        if (id > 0) notesRepository.observeNote(id) else flowOf(NoteEntity(title = "", content = ""))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val attachments = _currentNoteId.flatMapLatest { id ->
        if (id > 0) notesRepository.observeAttachments(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(title: String, content: String, checklistJson: String?, reminderAt: Long?) {
        viewModelScope.launch {
            val id = _currentNoteId.value
            val existing = if (id > 0) notesRepository.getNote(id) else null
            val entity = (existing ?: NoteEntity(title = title, content = content)).copy(
                title = title,
                content = content,
                isChecklist = checklistJson != null,
                checklistJson = checklistJson,
                reminderAt = reminderAt,
                updatedAt = System.currentTimeMillis()
            )
            val savedId = notesRepository.saveNote(entity)
            _currentNoteId.value = savedId
            if (reminderAt != null) {
                reminderScheduler.schedule(savedId, title.ifBlank { "Заметка" }, reminderAt)
            } else if (id > 0) {
                reminderScheduler.cancel(id)
            }
        }
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val id = _currentNoteId.value
            val count = if (id > 0) notesRepository.attachmentCount(id) else 0
            if (count >= AttachmentStorage.MAX_ATTACHMENTS) return@launch
            var noteId = id
            if (noteId <= 0) {
                noteId = notesRepository.saveNote(NoteEntity(title = "", content = ""))
                _currentNoteId.value = noteId
            }
            val attachment = attachmentStorage.saveCompressedImage(uri, noteId) ?: return@launch
            notesRepository.saveAttachment(attachment)
        }
    }

    fun deleteAttachment(id: Long, path: String) {
        viewModelScope.launch {
            attachmentStorage.deleteFile(path)
            notesRepository.deleteAttachment(id)
        }
    }

    fun createChecklistItem(text: String): ChecklistItem {
        return ChecklistItem(id = UUID.randomUUID().toString(), text = text)
    }

    fun encodeChecklist(items: List<ChecklistItem>): String = json.encodeToString(items)
}
