package com.notes.vault.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.notes.vault.data.local.AttachmentDao
import com.notes.vault.data.local.DatabaseProvider
import com.notes.vault.data.local.GroupDao
import com.notes.vault.data.local.NoteDao
import com.notes.vault.data.local.VaultEntryDao
import com.notes.vault.data.model.AttachmentEntity
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteGroupEntity
import com.notes.vault.data.model.VaultEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider
) {
    private val db get() = databaseProvider.getNotesDatabase()
    private val groupDao: GroupDao get() = db.groupDao()
    private val noteDao: NoteDao get() = db.noteDao()
    private val attachmentDao: AttachmentDao get() = db.attachmentDao()

    fun observeGroups(): Flow<List<NoteGroupEntity>> = groupDao.observeRegularGroups()
    fun observeAllNotes(): Flow<List<NoteEntity>> = noteDao.observeAllRegular()
    fun observeNotesByGroup(groupId: Long): Flow<List<NoteEntity>> = noteDao.observeByGroup(groupId)
    fun observeNote(id: Long): Flow<NoteEntity?> = noteDao.observeById(id)
    fun observeAttachments(noteId: Long): Flow<List<AttachmentEntity>> = attachmentDao.observeByNote(noteId)

    suspend fun getGroup(id: Long) = groupDao.getById(id)
    suspend fun getNote(id: Long) = noteDao.getById(id)

    suspend fun saveGroup(group: NoteGroupEntity): Long = groupDao.insert(group)
    suspend fun updateGroup(group: NoteGroupEntity) = groupDao.update(group)
    suspend fun deleteGroup(id: Long) = groupDao.delete(id)

    suspend fun saveNote(note: NoteEntity): Long = noteDao.insert(note)
    suspend fun updateNote(note: NoteEntity) = noteDao.update(note)
    suspend fun deleteNote(id: Long) = noteDao.delete(id)

    suspend fun saveAttachment(attachment: AttachmentEntity): Long = attachmentDao.insert(attachment)
    suspend fun deleteAttachment(id: Long) = attachmentDao.delete(id)
    suspend fun attachmentCount(noteId: Long) = attachmentDao.countByNote(noteId)

    suspend fun getAllGroups() = groupDao.getAll(isVault = false)
    suspend fun getAllNotes() = noteDao.getAllRegular()
    suspend fun getAllAttachments() = attachmentDao.getAll()
    suspend fun getUpcomingReminders(now: Long) = noteDao.getUpcomingReminders(now)
}

@Singleton
class VaultRepository @Inject constructor(
    private val vaultSession: VaultSessionManager
) {
    private val groupDao: GroupDao
        get() = vaultSession.vaultGroupDao() ?: throw IllegalStateException("Vault is locked")
    private val vaultEntryDao: VaultEntryDao
        get() = vaultSession.vaultEntryDao() ?: throw IllegalStateException("Vault is locked")

    fun observeGroups(): Flow<List<NoteGroupEntity>> = groupDao.observeVaultGroups()
    fun observeAllEntries(): Flow<List<VaultEntryEntity>> = vaultEntryDao.observeAll()
    fun observeEntriesByGroup(groupId: Long): Flow<List<VaultEntryEntity>> =
        vaultEntryDao.observeByGroup(groupId)
    fun observeEntry(id: Long): Flow<VaultEntryEntity?> = vaultEntryDao.observeById(id)

    suspend fun getEntry(id: Long) = vaultEntryDao.getById(id)
    suspend fun saveEntry(entry: VaultEntryEntity): Long = vaultEntryDao.insert(entry)
    suspend fun updateEntry(entry: VaultEntryEntity) = vaultEntryDao.update(entry)
    suspend fun deleteEntry(id: Long) = vaultEntryDao.delete(id)
    suspend fun saveGroup(group: NoteGroupEntity): Long = groupDao.insert(group)
    suspend fun getAllGroups() = groupDao.getAll(isVault = true)
    suspend fun getAllEntries() = vaultEntryDao.getAll()
}

@Singleton
class AttachmentStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_ATTACHMENTS = 10
        const val MAX_DIMENSION = 1200
        const val JPEG_QUALITY = 85
    }

    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments").also { it.mkdirs() }

    suspend fun saveCompressedImage(uri: Uri, noteId: Long): AttachmentEntity? = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val original = BitmapFactory.decodeStream(input) ?: return@withContext null
        input.close()

        val (width, height) = original.width to original.height
        val scale = minOf(MAX_DIMENSION.toFloat() / width, MAX_DIMENSION.toFloat() / height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            original
        }

        val fileName = "${noteId}_${UUID.randomUUID()}.jpg"
        val file = File(attachmentsDir, fileName)
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (scaled !== original) scaled.recycle()
        original.recycle()

        AttachmentEntity(
            noteId = noteId,
            fileName = fileName,
            filePath = file.absolutePath
        )
    }

    fun deleteFile(path: String) {
        File(path).delete()
    }
}
