package io.github.ketarro.credivaro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0C5A5A), onPrimary = Color.White,
    primaryContainer = Color(0xFFC5ECE6), onPrimaryContainer = Color(0xFF102D36),
    secondary = Color(0xFF385E60), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8ECEA), onSecondaryContainer = Color(0xFF102D36),
    tertiary = Color(0xFF176B3A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0F3D7), onTertiaryContainer = Color(0xFF124926),
    background = Color(0xFFF5F7FA), onBackground = Color(0xFF102D36),
    surface = Color.White, onSurface = Color(0xFF102D36),
    surfaceVariant = Color(0xFFE8EFF0), onSurfaceVariant = Color(0xFF425B63),
    surfaceTint = Color(0xFF0C5A5A), surfaceDim = Color(0xFFDCE5E7), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F5F6),
    surfaceContainer = Color(0xFFE8EFF0), surfaceContainerHigh = Color(0xFFE0E9EA),
    surfaceContainerHighest = Color(0xFFD8E4E5),
    inverseSurface = Color(0xFF17343D), inverseOnSurface = Color(0xFFF5F7FA),
    inversePrimary = Color(0xFF80D5CA),
    outline = Color(0xFF647C83), outlineVariant = Color(0xFFBFCED0),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    scrim = Color.Black
)
private val Dark = darkColorScheme(
    primary = Color(0xFF80D5CA), onPrimary = Color(0xFF102D36),
    primaryContainer = Color(0xFF17504F), onPrimaryContainer = Color(0xFFE0F8F3),
    secondary = Color(0xFFA9D1CE), onSecondary = Color(0xFF102D36),
    secondaryContainer = Color(0xFF28534F), onSecondaryContainer = Color(0xFFE0F8F3),
    tertiary = Color(0xFF48E27A), onTertiary = Color(0xFF102D36),
    tertiaryContainer = Color(0xFF184D31), onTertiaryContainer = Color(0xFFB5F5C5),
    background = Color(0xFF102D36), onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF17343D), onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF28464F), onSurfaceVariant = Color(0xFFD9DEE2),
    surfaceTint = Color(0xFF80D5CA), surfaceDim = Color(0xFF102D36), surfaceBright = Color(0xFF37545D),
    surfaceContainerLowest = Color(0xFF0B242C), surfaceContainerLow = Color(0xFF132F38),
    surfaceContainer = Color(0xFF17343D), surfaceContainerHigh = Color(0xFF203E47),
    surfaceContainerHighest = Color(0xFF28464F),
    inverseSurface = Color(0xFFE8EFF0), inverseOnSurface = Color(0xFF102D36),
    inversePrimary = Color(0xFF0C5A5A),
    outline = Color(0xFFA9BABF), outlineVariant = Color(0xFF425B63),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black
)
@Composable
fun CredivaroTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
