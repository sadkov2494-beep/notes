package com.notes.vault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.notes.vault.ui.navigation.ThemedNavHost
import com.notes.vault.ui.screens.VaultUnlockScreen
import com.notes.vault.security.VaultSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var vaultSession: VaultSessionManager

    private var showLockOverlay by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openNoteId = intent?.getLongExtra(
            com.notes.vault.worker.ReminderWorker.EXTRA_NOTE_ID,
            -1L
        ) ?: -1L

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (vaultSession.isUnlocked) showLockOverlay = true
                        }
                        Lifecycle.Event.ON_START -> {
                            // Overlay stays until unlock
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            ThemedNavHost()

            if (showLockOverlay && vaultSession.isCreated) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        VaultUnlockScreen(
                            onUnlocked = { showLockOverlay = false },
                            onSetup = { showLockOverlay = false }
                        )
                    }
                }
            }
        }
    }
}
