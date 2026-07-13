package com.notes.vault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notes.vault.data.model.ColorStyle
import com.notes.vault.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val COLOR_STYLE = stringPreferencesKey("color_style")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.AUTO
        }
    }

    val colorStyle: Flow<ColorStyle> = context.dataStore.data.map { prefs ->
        runCatching { ColorStyle.valueOf(prefs[COLOR_STYLE] ?: ColorStyle.WARM_BROWN.name) }
            .getOrDefault(ColorStyle.WARM_BROWN)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }

    suspend fun setColorStyle(style: ColorStyle) {
        context.dataStore.edit { prefs ->
            prefs[COLOR_STYLE] = style.name
        }
    }
}
