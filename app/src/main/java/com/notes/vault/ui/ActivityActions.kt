package com.notes.vault.ui

import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import com.notes.vault.data.model.ImportMode

data class ImagePickRequest(
    val noteId: Long,
    val onResult: (Uri) -> Unit
)

data class AppActions(
    val pickImageFromGallery: (ImagePickRequest) -> Unit = {},
    val takePhotoWithCamera: (ImagePickRequest) -> Unit = {},
    val requestExport: () -> Unit = {},
    val requestImport: () -> Unit = {},
    val requestShareExport: () -> Unit = {}
)

val LocalAppActions = compositionLocalOf { AppActions() }

sealed class BackupDialogState {
    data object Hidden : BackupDialogState()
    data object ExportPassword : BackupDialogState()
    data object ImportPassword : BackupDialogState()
    data class ImportMode(val file: java.io.File, val password: CharArray) : BackupDialogState()
    data class Message(val text: String, val isError: Boolean = false) : BackupDialogState()
}
