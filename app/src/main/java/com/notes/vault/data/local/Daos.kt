package com.notes.vault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notes.vault.data.model.AttachmentEntity
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteGroupEntity
import com.notes.vault.data.model.VaultEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE isVault = 0 ORDER BY name ASC")
    fun observeRegularGroups(): Flow<List<NoteGroupEntity>>

    @Query("SELECT * FROM groups WHERE isVault = 1 ORDER BY name ASC")
    fun observeVaultGroups(): Flow<List<NoteGroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): NoteGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: NoteGroupEntity): Long

    @Update
    suspend fun update(group: NoteGroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM groups WHERE isVault = :isVault")
    suspend fun getAll(isVault: Boolean): List<NoteGroupEntity>
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isVault = 0 AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAllRegular(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE groupId = :groupId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeByGroup(groupId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isVault = 0 AND deletedAt IS NULL AND groupId IS NULL ORDER BY createdAt DESC")
    fun observeUngrouped(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isVault = 0 AND deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE notes SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restore(id: Long, updatedAt: Long)

    @Query("SELECT * FROM notes WHERE isVault = 0 AND deletedAt IS NULL")
    suspend fun getAllRegular(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND reminderAt > :now AND deletedAt IS NULL")
    suspend fun getUpcomingReminders(now: Long): List<NoteEntity>
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun observeByNote(noteId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun getByNote(noteId: Long): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM attachments WHERE noteId = :noteId")
    suspend fun countByNote(noteId: Long): Int

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>
}

@Dao
interface VaultEntryDao {
    @Query("SELECT * FROM vault_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE groupId = :groupId ORDER BY updatedAt DESC")
    fun observeByGroup(groupId: Long): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getById(id: Long): VaultEntryEntity?

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    fun observeById(id: Long): Flow<VaultEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntryEntity): Long

    @Update
    suspend fun update(entry: VaultEntryEntity)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM vault_entries")
    suspend fun getAll(): List<VaultEntryEntity>
}
