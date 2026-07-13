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

enum class ColorStyle {
    SYSTEM,
    WARM_BROWN,
    HONEY,
    FOREST,
    SKY,
    BERRY
}

enum class VaultUnlockMode {
    PASSWORD,
    PIN
}

enum class ImportMode {
    REPLACE,
    MERGE
}
