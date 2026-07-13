package com.notes.vault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.notes.vault.data.model.ColorStyle
import com.notes.vault.data.model.ThemeMode

private val WarmBrownLight = lightColorScheme(
    primary = Color(0xFF8B5E3C),
    onPrimary = Color(0xFFFFF8F0),
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Color(0xFFC47B3A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE2C4),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF7A9B5A),
    onTertiary = Color.White,
    background = Color(0xFFFFF6EC),
    onBackground = Color(0xFF2B1B10),
    surface = Color(0xFFFFF9F2),
    onSurface = Color(0xFF2B1B10),
    surfaceVariant = Color(0xFFF0E0D0),
    onSurfaceVariant = Color(0xFF564235)
)

private val WarmBrownDark = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C16),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFE8B27A),
    onSecondary = Color(0xFF432B00),
    background = Color(0xFF1F1510),
    onBackground = Color(0xFFF5E6D8),
    surface = Color(0xFF261A14),
    onSurface = Color(0xFFF5E6D8),
    surfaceVariant = Color(0xFF3D2B20),
    onSurfaceVariant = Color(0xFFD8C3B0)
)

private val HoneyLight = lightColorScheme(
    primary = Color(0xFFE39B2B),
    onPrimary = Color(0xFF2B1800),
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFFD97B3D),
    background = Color(0xFFFFF8E7),
    onBackground = Color(0xFF2A2108),
    surface = Color(0xFFFFFBF0),
    onSurface = Color(0xFF2A2108)
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF4F7A45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6B8),
    onPrimaryContainer = Color(0xFF10200A),
    secondary = Color(0xFF6B8F71),
    background = Color(0xFFF3FAF0),
    onBackground = Color(0xFF152014),
    surface = Color(0xFFF7FCF5),
    onSurface = Color(0xFF152014)
)

private val SkyLight = lightColorScheme(
    primary = Color(0xFF3D7EA6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6F8),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF5B9BB8),
    background = Color(0xFFF2F8FC),
    onBackground = Color(0xFF121B22),
    surface = Color(0xFFF7FBFE),
    onSurface = Color(0xFF121B22)
)

private val BerryLight = lightColorScheme(
    primary = Color(0xFFB04A6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD0DE),
    onPrimaryContainer = Color(0xFF3A071C),
    secondary = Color(0xFFD06B8B),
    background = Color(0xFFFFF5F8),
    onBackground = Color(0xFF2A121A),
    surface = Color(0xFFFFFAFB),
    onSurface = Color(0xFF2A121A)
)

private fun colorSchemeFor(style: ColorStyle, dark: Boolean): ColorScheme {
    return when (style) {
        ColorStyle.WARM_BROWN -> if (dark) WarmBrownDark else WarmBrownLight
        ColorStyle.HONEY -> if (dark) WarmBrownDark else HoneyLight
        ColorStyle.FOREST -> if (dark) darkColorScheme(primary = Color(0xFFA0D08C)) else ForestLight
        ColorStyle.SKY -> if (dark) darkColorScheme(primary = Color(0xFF8DC8E8)) else SkyLight
        ColorStyle.BERRY -> if (dark) darkColorScheme(primary = Color(0xFFF0A0BB)) else BerryLight
        ColorStyle.SYSTEM -> if (dark) darkColorScheme() else lightColorScheme()
    }
}

@Composable
fun NotesVaultTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    colorStyle: ColorStyle = ColorStyle.WARM_BROWN,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = when {
        colorStyle == ColorStyle.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> colorSchemeFor(colorStyle, darkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
