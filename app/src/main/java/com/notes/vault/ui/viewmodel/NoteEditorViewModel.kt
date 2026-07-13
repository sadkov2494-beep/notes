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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
    private val attachmentStorage: AttachmentStorage,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val json = Json { ignoreUnknownKeys = true }

    val note = if (noteId > 0) {
        notesRepository.observeNote(noteId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(
            NoteEntity(title = "", content = "")
        )
    }

    val attachments = if (noteId > 0) {
        notesRepository.observeAttachments(noteId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(emptyList<AttachmentEntity>())
    }

    fun save(title: String, content: String, checklistJson: String?, reminderAt: Long?) {
        viewModelScope.launch {
            val existing = if (noteId > 0) notesRepository.getNote(noteId) else null
            val entity = (existing ?: NoteEntity(title = title, content = content)).copy(
                title = title,
                content = content,
                isChecklist = checklistJson != null,
                checklistJson = checklistJson,
                reminderAt = reminderAt,
                updatedAt = System.currentTimeMillis()
            )
            val id = notesRepository.saveNote(entity)
            if (reminderAt != null) {
                reminderScheduler.schedule(id, title.ifBlank { "Заметка" }, reminderAt)
            } else if (noteId > 0) {
                reminderScheduler.cancel(noteId)
            }
        }
    }

    fun addAttachment(uri: Uri, currentNoteId: Long, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val count = notesRepository.attachmentCount(currentNoteId)
            if (count >= AttachmentStorage.MAX_ATTACHMENTS) return@launch
            var id = currentNoteId
            if (id <= 0) {
                id = notesRepository.saveNote(NoteEntity(title = "", content = ""))
            }
            val attachment = attachmentStorage.saveCompressedImage(uri, id) ?: return@launch
            notesRepository.saveAttachment(attachment)
            onSaved(id)
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
