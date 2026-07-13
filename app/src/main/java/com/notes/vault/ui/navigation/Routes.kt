package com.notes.vault.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
data class Group(val groupId: Long, val isVault: Boolean = false)

@Serializable
data class NoteEditor(val noteId: Long = 0L)

@Serializable
object VaultUnlock

@Serializable
object VaultSetup

@Serializable
data class VaultEntryEditor(val entryId: Long = 0L)

@Serializable
object Settings

@Serializable
object VaultHome

@Serializable
object DeletedHistory
