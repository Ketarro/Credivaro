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
    tertiary = Color(0xFF176B3A), onTertiary = Color.White,
    background = Color(0xFFF5F7FA), onBackground = Color(0xFF102D36),
    surface = Color.White, onSurface = Color(0xFF102D36),
    surfaceVariant = Color(0xFFE8EFF0), onSurfaceVariant = Color(0xFF425B63),
    outline = Color(0xFF647C83), error = Color(0xFFB3261E)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF80D5CA), onPrimary = Color(0xFF102D36),
    primaryContainer = Color(0xFF17504F), onPrimaryContainer = Color(0xFFE0F8F3),
    secondary = Color(0xFFA9D1CE), onSecondary = Color(0xFF102D36),
    tertiary = Color(0xFF48E27A), onTertiary = Color(0xFF102D36),
    background = Color(0xFF102D36), onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF17343D), onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF28464F), onSurfaceVariant = Color(0xFFD9DEE2),
    outline = Color(0xFFA9BABF), error = Color(0xFFFFB4AB)
)
@Composable
fun CredivaroTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
