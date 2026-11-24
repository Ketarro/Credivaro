package io.github.ketarro.credivaro

import android.os.Bundle
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.clickable
import io.github.ketarro.credivaro.model.Obligation
import android.util.Log
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        val initialAppearance = Appearance.fromStored(preferences.getString("mode", null))
        val initialDark = when (initialAppearance) {
            Appearance.LIGHT -> false
            Appearance.DARK -> true
            Appearance.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        // Match the window to the saved theme before the first Compose frame.
        applyWindowAppearance(initialDark)
        // Avoid activity window enter/exit animations during locale transitions.
        window.setWindowAnimations(0)
        if (BuildConfig.DEBUG) Log.d("CredivaroLocale", "activity created")
        setContent {
            var appearance by remember { mutableStateOf(initialAppearance) }
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                Appearance.LIGHT -> false
                Appearance.DARK -> true
                Appearance.SYSTEM -> systemDark
            }
            val configuration = LocalConfiguration.current
            var language by remember { mutableStateOf(currentAppLanguage()) }
            LaunchedEffect(configuration) { language = currentAppLanguage() }
            LaunchedEffect(dark) { applyWindowAppearance(dark) }
            val systemLanguage = remember(configuration) {
                Resources.getSystem().configuration.locales[0]
            }
            val records = remember { mutableStateListOf<Obligation>() }
            CredivaroTheme(dark) {
                AlphaScreen(
                    dark = dark,
                    systemDark = systemDark,
                    systemLanguage = systemLanguage,
                    records = records,
                    appearance = appearance,
                    onAppearance = { value ->
                        preferences.edit().putString("mode", value.stored).apply()
                        appearance = value
                    },
                    language = language,
                    onLanguage = { value ->
                        if (value != currentAppLanguage()) {
                            if (BuildConfig.DEBUG) Log.d("CredivaroLocale", "locale change requested")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value.tag))
                            language = currentAppLanguage()
                        }
                    }
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (BuildConfig.DEBUG) Log.d("CredivaroLocale", "configuration changed")
    }

    private fun applyWindowAppearance(dark: Boolean) {
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.parseColor(
            if (dark) "#102D36" else "#F5F7FA"
        )))
        val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

private fun currentAppLanguage(): AppLanguage =
    AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().get(0)?.language)

@Composable
private fun AlphaScreen(
    dark: Boolean,
    systemDark: Boolean,
    systemLanguage: java.util.Locale,
    records: MutableList<Obligation>,
    appearance: Appearance, onAppearance: (Appearance) -> Unit,
    language: AppLanguage, onLanguage: (AppLanguage) -> Unit
) {
    var settings by rememberSaveable { mutableStateOf(false) }
    var about by rememberSaveable { mutableStateOf(false) }
    var obligations by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf("") }
    BackHandler(settings || about || obligations) {
        if (about) about = false else { settings = false; obligations = false }
    }
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            CredivaroBottomBar(
                settingsSelected = settings,
                obligationsSelected = obligations,
                onHome = { settings = false; about = false; obligations = false },
                onObligations = { obligations = true; settings = false; about = false },
                onSettings = { settings = true; about = false; obligations = false }
            )
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
                    BrandHeader(dark)
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
                    PersonalizationCard(appearance, systemDark, language, systemLanguage,
                        onTheme = { dialog = "theme" }, onLanguage = { dialog = "language" })
                    if (dialog == "theme") {
                        val labels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
                        PreferenceDialog(R.string.theme_label, labels.map { stringResource(it) }, appearance.ordinal,
                            onDismiss = { dialog = "" }, onSelect = { dialog = ""; onAppearance(Appearance.entries[it]) })
                    }
                    if (dialog == "language") {
                        val labels = listOf(R.string.language_system, R.string.language_ru, R.string.language_en)
                        PreferenceDialog(R.string.language_label, labels.map { stringResource(it) }, language.ordinal,
                            onDismiss = { dialog = "" }, onSelect = { dialog = ""; onLanguage(AppLanguage.entries[it]) })
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
                } else if (obligations) {
                    ObligationPreview(records)
                } else {
                    BrandHeader(dark)
                    Text(stringResource(R.string.tagline), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    PreviewDashboard(records) { obligations = true; settings = false; about = false }
                    AlphaNotice()
                }
            }
        }
    }
}

@Composable
private fun CredivaroBottomBar(
    settingsSelected: Boolean,
    obligationsSelected: Boolean,
    onObligations: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.55f))
            Row(
                Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactDestination(
                    modifier = Modifier.weight(1f),
                    selected = !settingsSelected && !obligationsSelected,
                    icon = R.drawable.ic_home,
                    label = stringResource(R.string.home),
                    onClick = onHome
                )
                CompactDestination(
                    modifier = Modifier.weight(1f),
                    selected = obligationsSelected,
                    icon = R.drawable.ic_wallet,
                    label = stringResource(R.string.obligations_nav),
                    onClick = onObligations
                )
                CompactDestination(
                    modifier = Modifier.weight(1f),
                    selected = settingsSelected,
                    icon = R.drawable.ic_settings,
                    label = stringResource(R.string.settings),
                    onClick = onSettings
                )
            }
        }
    }
}

@Composable
private fun CompactDestination(
    modifier: Modifier,
    selected: Boolean,
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (selected) colors.primary else colors.onSurfaceVariant
    Column(
        modifier.heightIn(min = 64.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Box(
            Modifier.width(22.dp).height(3.dp).background(
                if (selected) colors.primary else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(2.dp)
            )
        )
        Icon(painterResource(icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Text(
            label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BrandHeader(dark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(
            painterResource(if (dark) R.drawable.signal_frame_dark else R.drawable.signal_frame_light),
            null, Modifier.size(36.dp)
        )
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
private fun PersonalizationCard(
    appearance: Appearance, systemDark: Boolean, language: AppLanguage,
    systemLanguage: java.util.Locale, onTheme: () -> Unit, onLanguage: () -> Unit
) {
    val theme = when (appearance) {
        Appearance.SYSTEM -> stringResource(R.string.system_value, stringResource(R.string.theme_system),
            stringResource(if (systemDark) R.string.theme_dark else R.string.theme_light))
        Appearance.LIGHT -> stringResource(R.string.theme_light)
        Appearance.DARK -> stringResource(R.string.theme_dark)
    }
    val systemName = when (systemLanguage.language) {
        "ru" -> stringResource(R.string.language_ru)
        "en" -> stringResource(R.string.language_en)
        else -> systemLanguage.getDisplayLanguage(systemLanguage).ifBlank { systemLanguage.language }
    }
    val languageValue = when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.system_value, stringResource(R.string.language_system), systemName)
        AppLanguage.RUSSIAN -> stringResource(R.string.language_ru)
        AppLanguage.ENGLISH -> stringResource(R.string.language_en)
    }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            PreferenceRow(R.string.theme_label, theme, R.drawable.ic_theme, onTheme)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceRow(R.string.language_label, languageValue, R.drawable.ic_language, onLanguage)
        }
    }
}

@Composable
private fun PreferenceRow(label: Int, value: String, icon: Int, action: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = action).heightIn(min = 64.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), null, tint = colors.primary)
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(value, Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant)
        Icon(painterResource(R.drawable.ic_chevron), null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun PreferenceDialog(title: Int, labels: List<String>, selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(title)) }, text = {
        Column(Modifier.selectableGroup()) {
            labels.forEachIndexed { index, label -> ChoiceRow(label, selected == index) { onSelect(index) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun AlphaNotice() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(R.drawable.ic_info), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.alpha_short), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
