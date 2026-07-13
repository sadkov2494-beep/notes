package com.notes.vault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.notes.vault.ui.LocalAppActions
import com.notes.vault.ui.screens.GroupScreen
import com.notes.vault.ui.screens.HomeScreen
import com.notes.vault.ui.screens.NoteEditorScreen
import com.notes.vault.ui.screens.SettingsScreen
import com.notes.vault.ui.screens.VaultEntryEditorScreen
import com.notes.vault.ui.screens.VaultHomeScreen
import com.notes.vault.ui.screens.VaultSetupScreen
import com.notes.vault.ui.screens.VaultUnlockScreen
import com.notes.vault.ui.viewmodel.SettingsViewModel

@Composable
fun NotesNavHost(openNoteId: Long = -1L) {
    val navController = rememberNavController()
    val appActions = LocalAppActions.current

    LaunchedEffect(openNoteId) {
        if (openNoteId > 0) {
            navController.navigate(NoteEditor(openNoteId))
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        val quickNote = (context as? android.app.Activity)
            ?.intent?.getBooleanExtra(com.notes.vault.widget.EXTRA_QUICK_NOTE, false) == true
        if (quickNote) {
            navController.navigate(NoteEditor())
        }
    }

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            HomeScreen(
                onOpenGroup = { groupId, isVault ->
                    navController.navigate(Group(groupId, isVault))
                },
                onOpenVault = { navController.navigate(VaultUnlock) },
                onOpenSettings = { navController.navigate(Settings) }
            )
        }
        composable<Group> { backStackEntry ->
            val route = backStackEntry.toRoute<Group>()
            GroupScreen(
                groupId = route.groupId,
                isVault = route.isVault,
                onBack = { navController.popBackStack() },
                onOpenNote = { id ->
                    if (route.isVault) navController.navigate(VaultEntryEditor(id))
                    else navController.navigate(NoteEditor(id))
                }
            )
        }
        composable<VaultUnlock> {
            VaultUnlockScreen(
                onUnlocked = {
                    navController.navigate(VaultHome) {
                        popUpTo(VaultUnlock) { inclusive = true }
                    }
                },
                onSetup = {
                    navController.navigate(VaultSetup) {
                        popUpTo(VaultUnlock) { inclusive = true }
                    }
                }
            )
        }
        composable<VaultSetup> {
            VaultSetupScreen(
                onComplete = {
                    navController.navigate(VaultHome) {
                        popUpTo(VaultSetup) { inclusive = true }
                    }
                }
            )
        }
        composable<VaultHome> {
            VaultHomeScreen(
                onBack = { navController.popBackStack() },
                onOpenEntry = { navController.navigate(VaultEntryEditor(it)) },
                onCreateEntry = { navController.navigate(VaultEntryEditor()) }
            )
        }
        composable<NoteEditor> { backStackEntry ->
            val route = backStackEntry.toRoute<NoteEditor>()
            NoteEditorScreen(
                noteId = route.noteId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<VaultEntryEditor> { backStackEntry ->
            val route = backStackEntry.toRoute<VaultEntryEditor>()
            VaultEntryEditorScreen(
                entryId = route.entryId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onExport = appActions.requestExport,
                onShareExport = appActions.requestShareExport,
                onImport = appActions.requestImport
            )
        }
    }
}

@Composable
fun ThemedNavHost(
    openNoteId: Long = -1L,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    com.notes.vault.ui.theme.NotesVaultTheme(themeMode = themeMode) {
        NotesNavHost(openNoteId = openNoteId)
    }
}
