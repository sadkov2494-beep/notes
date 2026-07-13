package com.notes.vault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import com.notes.vault.ui.AppContent
import com.notes.vault.security.VaultSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @Inject lateinit var vaultSession: VaultSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openNoteId = intent?.getLongExtra(
            com.notes.vault.worker.ReminderWorker.EXTRA_NOTE_ID,
            -1L
        ) ?: -1L

        setContent {
            AppContent(
                vaultSession = vaultSession,
                openNoteId = openNoteId
            )
        }
    }
}
