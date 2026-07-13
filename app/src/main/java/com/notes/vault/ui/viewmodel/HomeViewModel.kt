package com.notes.vault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.FragmentActivity
import com.notes.vault.data.model.NoteEntity
import com.notes.vault.data.model.NoteGroupEntity
import com.notes.vault.data.model.NoteSection
import com.notes.vault.data.repository.NotesRepository
import com.notes.vault.data.repository.VaultRepository
import com.notes.vault.security.VaultCryptoManager
import com.notes.vault.security.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
    val section: NoteSection = NoteSection.ALL_NOTES,
    val groups: List<NoteGroupEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val vaultUnlocked: Boolean = false,
    val vaultCreated: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val vaultSession: VaultSessionManager
) : ViewModel() {

    private val section = MutableStateFlow(NoteSection.ALL_NOTES)

    val uiState: StateFlow<HomeUiState> = combine(
        section,
        notesRepository.observeGroups(),
        notesRepository.observeAllNotes()
    ) { currentSection, groups, notes ->
        HomeUiState(
            section = currentSection,
            groups = groups,
            notes = notes,
            vaultUnlocked = vaultSession.isUnlocked,
            vaultCreated = vaultSession.isCreated,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setSection(newSection: NoteSection) {
        section.value = newSection
    }

    fun createGroup(name: String, colorArgb: Int, iconName: String, isVault: Boolean) {
        viewModelScope.launch {
            notesRepository.saveGroup(
                NoteGroupEntity(
                    name = name,
                    colorArgb = colorArgb,
                    iconName = iconName,
                    isVault = isVault
                )
            )
        }
    }
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val vaultSession: VaultSessionManager,
    private val cryptoManager: VaultCryptoManager
) : ViewModel() {

    val entries = kotlinx.coroutines.flow.flow {
        if (vaultSession.isUnlocked) {
            emitAll(vaultRepository.observeAll())
        } else {
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError = _unlockError.asStateFlow()

    val isUnlocked: Boolean get() = vaultSession.isUnlocked
    val isCreated: Boolean get() = vaultSession.isCreated
    val biometricAvailable: Boolean get() = cryptoManager.isBiometricAvailable()
    val biometricKeyValid: Boolean get() = cryptoManager.isBiometricKeyValid()

    fun unlockWithPassword(password: String): Boolean {
        val success = vaultSession.unlockWithPassword(password)
        if (!success) _unlockError.value = "Неверный пароль"
        return success
    }

    fun createVault(password: String, confirm: String, recovery: String): String? {
        if (password.length < 4) return "Пароль слишком короткий"
        if (password != confirm) return "Пароли не совпадают"
        if (recovery.length < 5) return "Фраза восстановления: мин. 5 символов"
        vaultSession.createVault(password, recovery)
        return null
    }

    fun enableBiometric() = vaultSession.enableBiometric()

    fun lock() = vaultSession.lock()

    fun resetPassword(recovery: String, newPassword: String): Boolean {
        return vaultSession.resetPassword(newPassword, recovery)
    }

    fun clearError() {
        _unlockError.value = null
    }

    suspend fun unlockWithBiometric(activity: FragmentActivity): Boolean {
        if (!biometricKeyValid) {
            _unlockError.value = "Биометрия недоступна. Введите пароль."
            return false
        }
        return cryptoManager.authenticateBiometric(
            activity = activity,
            title = "Сейф",
            subtitle = "Подтвердите отпечаток пальца"
        ).fold(
            onSuccess = { key ->
                vaultSession.unlockWithKey(key)
                true
            },
            onFailure = {
                _unlockError.value = it.message ?: "Ошибка биометрии"
                false
            }
        )
    }
}
