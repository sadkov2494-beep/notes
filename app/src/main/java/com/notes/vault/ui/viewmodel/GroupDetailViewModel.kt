package com.notes.vault.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.notes.vault.data.repository.NotesRepository
import com.notes.vault.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {
    fun observeNotes(groupId: Long) = notesRepository.observeNotesByGroup(groupId)
    fun observeVaultEntries(groupId: Long) = vaultRepository.observeEntriesByGroup(groupId)
}
