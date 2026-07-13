package com.notes.vault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notes.vault.data.model.AttachmentEntity
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteGroupEntity
import com.notes.vault.data.model.VaultEntryEntity

@Database(
    entities = [
        NoteGroupEntity::class,
        NoteEntity::class,
        AttachmentEntity::class,
        VaultEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
}

@Database(
    entities = [
        NoteGroupEntity::class,
        NoteEntity::class,
        AttachmentEntity::class,
        VaultEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun vaultEntryDao(): VaultEntryDao
}
