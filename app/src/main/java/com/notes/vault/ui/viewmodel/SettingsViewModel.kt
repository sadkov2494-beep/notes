package com.notes.vault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notes.vault.data.SettingsRepository
import com.notes.vault.data.export.BackupManager
import com.notes.vault.data.model.ImportMode
import com.notes.vault.data.model.ThemeMode
import com.notes.vault.security.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val vaultSession: VaultSessionManager
) : ViewModel() {

    val themeMode = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.AUTO)

    val colorStyle = settingsRepository.colorStyle
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.notes.vault.data.model.ColorStyle.WARM_BROWN
        )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setColorStyle(style: com.notes.vault.data.model.ColorStyle) {
        viewModelScope.launch { settingsRepository.setColorStyle(style) }
    }

    suspend fun export(file: File, password: CharArray): Result<Unit> {
        return backupManager.exportToFile(file, password, includeVault = vaultSession.isUnlocked)
    }

    suspend fun import(file: File, password: CharArray, mode: ImportMode): Result<Unit> {
        return backupManager.importFromFile(file, password, mode).map { }
    }
}
