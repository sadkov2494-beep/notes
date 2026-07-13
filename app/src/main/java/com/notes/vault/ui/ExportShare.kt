package com.notes.vault.ui

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.notes.vault.data.model.ImportMode
import java.io.File

enum class ExportMode {
    SAVE,
    SHARE
}

fun shareBackupFile(context: android.content.Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/x-nbk"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться архивом"))
}
