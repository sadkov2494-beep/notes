package com.notes.vault.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notes.vault.data.model.VaultEntryEntity
import com.notes.vault.data.repository.VaultRepository
import com.notes.vault.security.ClipboardManagerHelper
import com.notes.vault.security.PasswordGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val passwordGenerator: PasswordGenerator,
    private val clipboardHelper: ClipboardManagerHelper
) : ViewModel() {

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    val entry = if (entryId > 0) {
        vaultRepository.observeEntry(entryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(
            VaultEntryEntity(service = "", login = "", password = "")
        )
    }

    fun save(
        service: String,
        login: String,
        password: String,
        url: String,
        comment: String
    ) {
        viewModelScope.launch {
            val existing = if (entryId > 0) vaultRepository.getEntry(entryId) else null
            val entity = (existing ?: VaultEntryEntity(service = service, login = login, password = password))
                .copy(
                    service = service,
                    login = login,
                    password = password,
                    url = url,
                    comment = comment,
                    updatedAt = System.currentTimeMillis()
                )
            vaultRepository.saveEntry(entity)
        }
    }

    fun generatePassword(length: Int, uppercase: Boolean, digits: Boolean, special: Boolean): String {
        return passwordGenerator.generate(length, uppercase, digits, special)
    }

    fun copyPassword(password: String) {
        clipboardHelper.copyPassword(password)
    }
}
