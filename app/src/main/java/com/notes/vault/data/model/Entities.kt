package com.notes.vault.data.model

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "groups",
    indices = [Index("isVault")]
)
@Serializable
data class NoteGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconName: String,
    val isVault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = NoteGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId"), Index("isVault"), Index("reminderAt")]
)
@Serializable
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long? = null,
    val title: String,
    val content: String,
    val isVault: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistJson: String? = null,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
@Serializable
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String = "image/jpeg",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_entries",
    foreignKeys = [
        ForeignKey(
            entity = NoteGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
@Serializable
data class VaultEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long? = null,
    val service: String,
    val login: String,
    val password: String,
    val url: String = "",
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
