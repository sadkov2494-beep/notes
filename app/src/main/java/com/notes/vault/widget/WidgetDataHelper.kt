package com.notes.vault.widget

import android.content.Context
import com.notes.vault.data.local.NotesDatabase
import com.notes.vault.data.model.ChecklistItem
import com.notes.vault.data.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

object WidgetDataHelper {
    private val json = Json { ignoreUnknownKeys = true }

    private fun database(context: Context): NotesDatabase {
        return androidx.room.Room.databaseBuilder(
            context.applicationContext,
            NotesDatabase::class.java,
            "notes.db"
        ).build()
    }

    suspend fun saveQuickNote(context: Context, text: String) = withContext(Dispatchers.IO) {
        val db = database(context)
        try {
            val title = text.lineSequence().firstOrNull().orEmpty().take(80)
            val content = text.trim()
            db.noteDao().insert(
                NoteEntity(
                    title = title.ifBlank { "Быстрая заметка" },
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } finally {
            db.close()
        }
    }

    suspend fun getLatestChecklistNote(context: Context): Pair<NoteEntity, List<ChecklistItem>>? =
        withContext(Dispatchers.IO) {
            val db = database(context)
            try {
                val notes = db.noteDao().getAllRegular()
                    .filter { it.isChecklist && !it.checklistJson.isNullOrBlank() }
                    .sortedByDescending { it.updatedAt }
                val note = notes.firstOrNull() ?: return@withContext null
                val items = json.decodeFromString<List<ChecklistItem>>(note.checklistJson!!)
                note to items
            } finally {
                db.close()
            }
        }

    suspend fun toggleChecklistItem(context: Context, noteId: Long, itemId: String) =
        withContext(Dispatchers.IO) {
            val db = database(context)
            try {
                val note = db.noteDao().getById(noteId) ?: return@withContext
                val items = json.decodeFromString<List<ChecklistItem>>(note.checklistJson ?: return@withContext)
                val updated = items.map {
                    if (it.id == itemId) it.copy(checked = !it.checked) else it
                }
                db.noteDao().update(
                    note.copy(
                        checklistJson = json.encodeToString(updated),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } finally {
                db.close()
            }
        }
}
