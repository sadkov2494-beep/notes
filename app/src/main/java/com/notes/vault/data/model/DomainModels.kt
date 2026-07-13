package com.notes.vault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)

enum class NoteSection {
    ALL_NOTES,
    VAULT
}

enum class ThemeMode {
    AUTO,
    LIGHT,
    DARK
}

enum class ImportMode {
    REPLACE,
    MERGE
}
