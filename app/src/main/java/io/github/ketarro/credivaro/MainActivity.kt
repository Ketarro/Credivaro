package io.github.ketarro.credivaro

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ketarro.credivaro.ui.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            var appearance by remember { mutableStateOf(Appearance.fromStored(preferences.getString("mode", null))) }
            val dark = when (appearance) {
                Appearance.LIGHT -> false
                Appearance.DARK -> true
                Appearance.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CredivaroTheme(dark) {
                AlphaScreen(
                    appearance = appearance,
                    onAppearance = { value ->
                        preferences.edit().putString("mode", value.stored).apply()
                        appearance = value
                    },
                    language = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().get(0)?.language),
                    onLanguage = { value ->
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value.tag))
                    }
                )
            }
        }
    }
}

@Composable
private fun AlphaScreen(
    appearance: Appearance, onAppearance: (Appearance) -> Unit,
    language: AppLanguage, onLanguage: (AppLanguage) -> Unit
) {
    var settings by rememberSaveable { mutableStateOf(false) }
    var about by rememberSaveable { mutableStateOf(false) }
    BackHandler(settings || about) { if (about) about = false else settings = false }
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSecondaryContainer,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.secondaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant
                )
                NavigationBarItem(selected = !settings, onClick = { settings = false; about = false },
                    icon = { Icon(painterResource(R.drawable.ic_home), null) },
                    label = { Text(stringResource(R.string.home)) }, colors = navColors)
                NavigationBarItem(selected = settings, onClick = { settings = true; about = false },
                    icon = { Icon(painterResource(R.drawable.ic_settings), null) },
                    label = { Text(stringResource(R.string.settings)) }, colors = navColors)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (about) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { about = false }) {
                            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                        }
                        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleLarge)
                    }
                    BrandHeader()
                    SettingsCard {
                        Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.build_label, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(color = colors.outlineVariant)
                        Text(stringResource(R.string.license_label))
                        Text(stringResource(R.string.about_description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    AlphaNotice()
                } else if (settings) {
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
                    SectionTitle(R.string.personalization)
                    SettingsCard {
                        Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.selectableGroup()) {
                            val labels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
                            Appearance.entries.forEachIndexed { index, mode ->
                                ChoiceRow(stringResource(labels[index]), appearance == mode) { onAppearance(mode) }
                            }
                        }
                        HorizontalDivider(color = colors.outlineVariant)
                        Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.selectableGroup()) {
                            val labels = listOf(R.string.language_system, R.string.language_ru, R.string.language_en)
                            AppLanguage.entries.forEachIndexed { index, value ->
                                ChoiceRow(stringResource(labels[index]), language == value) { if (language != value) onLanguage(value) }
                            }
                        }
                    }
                    SectionTitle(R.string.information)
                    Card(onClick = { about = true }, shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(painterResource(R.drawable.ic_info), null, tint = colors.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.about_summary), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            Icon(painterResource(R.drawable.ic_chevron), null, tint = colors.onSurfaceVariant)
                        }
                    }
                } else {
                    BrandHeader()
                    Text(stringResource(R.string.tagline), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Card(shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = colors.surface) {
                                Icon(painterResource(R.drawable.ic_wallet), null, tint = colors.primary, modifier = Modifier.padding(12.dp).size(28.dp))
                            }
                            Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.welcome_description), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    SettingsCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_shield), null, tint = colors.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.privacy_short), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    AlphaNotice()
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(44.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
            Text(stringResource(R.string.preview_label), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SectionTitle(id: Int) {
    Text(stringResource(id), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = action, role = Role.RadioButton).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AlphaNotice() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(R.drawable.ic_info), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.alpha_short), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
