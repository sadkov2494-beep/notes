package com.notes.vault.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.notes.vault.data.model.ImportMode
import com.notes.vault.security.VaultSessionManager
import com.notes.vault.ui.navigation.ThemedNavHost
import com.notes.vault.ui.screens.BackupMessageDialog
import com.notes.vault.ui.screens.CameraCaptureScreen
import com.notes.vault.ui.screens.ExportPasswordDialog
import com.notes.vault.ui.screens.ImportModeDialog
import com.notes.vault.ui.screens.ImportPasswordDialog
import com.notes.vault.ui.screens.VaultUnlockScreen
import com.notes.vault.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppContent(
    vaultSession: VaultSessionManager,
    openNoteId: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    var showLockOverlay by remember { mutableStateOf(false) }
    var backupDialog by remember { mutableStateOf<BackupDialogState>(BackupDialogState.Hidden) }
    var pendingImageRequest by remember { mutableStateOf<ImagePickRequest?>(null) }
    var cameraRequest by remember { mutableStateOf<ImagePickRequest?>(null) }
    var pendingExport by remember { mutableStateOf<Pair<CharArray, ExportMode>?>(null) }
    var pendingImportFile by remember { mutableStateOf<File?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingImageRequest?.onResult?.invoke(it) }
        pendingImageRequest = null
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-nbk")
    ) { uri: Uri? ->
        val export = pendingExport
        if (uri != null && export != null) {
            scope.launch {
                val tempFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.nbk")
                val result = settingsViewModel.export(tempFile, export.first)
                result.onSuccess {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        tempFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    tempFile.delete()
                    backupDialog = BackupDialogState.Message("Экспорт завершён")
                }.onFailure {
                    tempFile.delete()
                    backupDialog = BackupDialogState.Message(
                        it.message ?: "Ошибка экспорта",
                        isError = true
                    )
                }
                pendingExport = null
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.nbk")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            pendingImportFile = tempFile
            backupDialog = BackupDialogState.ImportPassword
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && vaultSession.isUnlocked) {
                showLockOverlay = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun startShareExport(password: CharArray) {
        scope.launch {
            val exportDir = File(context.cacheDir, "export").apply { mkdirs() }
            val tempFile = File(exportDir, "notes_backup_${System.currentTimeMillis()}.nbk")
            val result = settingsViewModel.export(tempFile, password)
            result.onSuccess {
                shareBackupFile(context, tempFile)
                backupDialog = BackupDialogState.Message("Архив готов к отправке")
            }.onFailure {
                tempFile.delete()
                backupDialog = BackupDialogState.Message(
                    it.message ?: "Ошибка экспорта",
                    isError = true
                )
            }
        }
    }

    val appActions = AppActions(
        pickImageFromGallery = { request ->
            pendingImageRequest = request
            pickImageLauncher.launch("image/*")
        },
        takePhotoWithCamera = { request -> cameraRequest = request },
        requestExport = { backupDialog = BackupDialogState.ExportPassword(ExportMode.SAVE) },
        requestImport = { openDocumentLauncher.launch(arrayOf("application/x-nbk", "*/*")) },
        requestShareExport = { backupDialog = BackupDialogState.ExportPassword(ExportMode.SHARE) }
    )

    CompositionLocalProvider(LocalAppActions provides appActions) {
        Box(Modifier.fillMaxSize()) {
            ThemedNavHost(openNoteId = openNoteId)

            if (showLockOverlay && vaultSession.isCreated) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VaultUnlockScreen(
                        onUnlocked = { showLockOverlay = false },
                        onSetup = { showLockOverlay = false }
                    )
                }
            }

            cameraRequest?.let { request ->
                CameraCaptureScreen(
                    onPhotoTaken = { uri ->
                        request.onResult(uri)
                        cameraRequest = null
                    },
                    onDismiss = { cameraRequest = null }
                )
            }
        }

        when (val state = backupDialog) {
            BackupDialogState.Hidden -> Unit
            is BackupDialogState.ExportPassword -> ExportPasswordDialog(
                onDismiss = { backupDialog = BackupDialogState.Hidden },
                onConfirm = { password ->
                    backupDialog = BackupDialogState.Hidden
                    when (state.mode) {
                        ExportMode.SAVE -> {
                            pendingExport = password to ExportMode.SAVE
                            createDocumentLauncher.launch("notes_backup_${System.currentTimeMillis()}.nbk")
                        }
                        ExportMode.SHARE -> startShareExport(password)
                    }
                }
            )
            BackupDialogState.ImportPassword -> ImportPasswordDialog(
                onDismiss = {
                    backupDialog = BackupDialogState.Hidden
                    pendingImportFile?.delete()
                    pendingImportFile = null
                },
                onConfirm = { password ->
                    val file = pendingImportFile
                    if (file != null) {
                        backupDialog = BackupDialogState.ImportMode(file, password)
                    }
                }
            )
            is BackupDialogState.ImportMode -> ImportModeDialog(
                onDismiss = {
                    backupDialog = BackupDialogState.Hidden
                    pendingImportFile?.delete()
                    pendingImportFile = null
                },
                onReplace = {
                    scope.launch {
                        val result = settingsViewModel.import(
                            state.file,
                            state.password,
                            ImportMode.REPLACE
                        )
                        state.file.delete()
                        pendingImportFile = null
                        backupDialog = result.fold(
                            onSuccess = { BackupDialogState.Message("Импорт завершён") },
                            onFailure = {
                                BackupDialogState.Message(it.message ?: "Ошибка импорта", true)
                            }
                        )
                    }
                },
                onMerge = {
                    scope.launch {
                        val result = settingsViewModel.import(
                            state.file,
                            state.password,
                            ImportMode.MERGE
                        )
                        state.file.delete()
                        pendingImportFile = null
                        backupDialog = result.fold(
                            onSuccess = { BackupDialogState.Message("Импорт завершён") },
                            onFailure = {
                                BackupDialogState.Message(it.message ?: "Ошибка импорта", true)
                            }
                        )
                    }
                }
            )
            is BackupDialogState.Message -> BackupMessageDialog(
                message = state.text,
                isError = state.isError,
                onDismiss = { backupDialog = BackupDialogState.Hidden }
            )
        }
    }
}
