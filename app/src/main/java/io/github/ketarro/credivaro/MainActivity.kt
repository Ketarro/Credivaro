package io.github.ketarro.credivaro

import android.os.Bundle
import android.os.Build
import android.os.LocaleList
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ketarro.credivaro.ui.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        StartupDiagnostics.mark("activity.create.begin")
        super.onCreate(savedInstanceState)
        StorageSession.start(applicationContext)
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
        StartupDiagnostics.mark("activity.create.end")
        setContent {
            var appearance by remember { mutableStateOf(initialAppearance) }
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                Appearance.LIGHT -> false
                Appearance.DARK -> true
                Appearance.SYSTEM -> systemDark
            }
            val configuration = LocalConfiguration.current
            var language by remember { mutableStateOf(currentAppLanguage(this@MainActivity)) }
            LaunchedEffect(configuration) { language = currentAppLanguage(this@MainActivity) }
            LaunchedEffect(dark) { applyWindowAppearance(dark) }
            val systemLanguage = remember(configuration) {
                Resources.getSystem().configuration.locales[0]
            }
            val storageState by StorageSession.state.collectAsState()
            LaunchedEffect(storageState is StorageState.Ready) {
                if (storageState is StorageState.Ready) {
                    withFrameNanos { }
                    StartupDiagnostics.mark("activity.storageReadyFrame")
                }
            }
            CredivaroTheme(dark) {
                AlphaScreen(
                    dark = dark,
                    systemDark = systemDark,
                    systemLanguage = systemLanguage,
                    storageState = storageState,
                    onRetryStorage = { StorageSession.start(applicationContext) },
                    appearance = appearance,
                    onAppearance = { value ->
                        preferences.edit().putString("mode", value.stored).apply()
                        appearance = value
                    },
                    language = language,
                    onLanguage = { value ->
                        if (value != currentAppLanguage(this@MainActivity)) {
                            StartupDiagnostics.mark("locale.request")
                            applyAppLanguage(this@MainActivity, value)
                            language = currentAppLanguage(this@MainActivity)
                            StartupDiagnostics.mark("locale.request.return")
                        }
                    }
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        StartupDiagnostics.mark("activity.configurationChanged")
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

// Android 13+ owns per-app language persistence; AppCompat retains the legacy path.
private fun currentAppLanguage(context: Context): AppLanguage =
    if (Build.VERSION.SDK_INT >= 33) {
        val locales = requireNotNull(context.getSystemService(LocaleManager::class.java)).applicationLocales
        AppLanguage.fromTag(if (locales.isEmpty) null else locales[0].language)
    } else AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().get(0)?.language)

private fun applyAppLanguage(context: Context, value: AppLanguage) {
    if (Build.VERSION.SDK_INT >= 33) {
        requireNotNull(context.getSystemService(LocaleManager::class.java)).applicationLocales = LocaleList.forLanguageTags(value.tag)
    } else AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value.tag))
}

@Composable
private fun AlphaScreen(
    dark: Boolean,
    systemDark: Boolean,
    systemLanguage: java.util.Locale,
    storageState: StorageState,
    onRetryStorage: () -> Unit,
    appearance: Appearance, onAppearance: (Appearance) -> Unit,
    language: AppLanguage, onLanguage: (AppLanguage) -> Unit
) {
    var settings by rememberSaveable { mutableStateOf(false) }
    var about by rememberSaveable { mutableStateOf(false) }
    var obligations by rememberSaveable { mutableStateOf(false) }
    var route by rememberSaveable { mutableStateOf("list") }
    // Do not reopen a possibly committed payment form after process/activity recreation.
    var paymentRequested by remember { mutableStateOf(false) }
    val holder = rememberSaveableStateHolder()
    val homeScroll = rememberScrollState()
    val listScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    val aboutScroll = rememberScrollState()
    val innerScroll = key(route) { rememberScrollState() }
    val screenScroll = when { about -> aboutScroll; settings -> settingsScroll; !obligations -> homeScroll; route == "list" -> listScroll; else -> innerScroll }
    fun openRoute(value: String, payment: Boolean = false) { route = value; paymentRequested = payment; obligations = true; settings = false; about = false }
    var dialog by rememberSaveable { mutableStateOf("") }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    var showSelfCheck by rememberSaveable { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf<AppLanguage?>(null) }
    LaunchedEffect(pendingLanguage) {
        val selected = pendingLanguage ?: return@LaunchedEffect
        // Close the separate dialog window before requesting system resource/config changes.
        withFrameNanos { }
        withFrameNanos { }
        pendingLanguage = null
        onLanguage(selected)
    }
    BackHandler(settings || about || obligations) {
        if (about) about = false else { settings = false; obligations = false }
    }
    val colors = MaterialTheme.colorScheme
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val editorOpen = obligations && (route == "new" || route.startsWith("edit:"))
    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (!keyboardVisible && !editorOpen) CredivaroBottomBar(
                settingsSelected = settings,
                obligationsSelected = obligations,
                onHome = { settings = false; about = false; obligations = false },
                onObligations = { if (obligations) { route = "list"; paymentRequested = false }; obligations = true; settings = false; about = false },
                onSettings = { settings = true; about = false; obligations = false }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().then(if (editorOpen) Modifier.fillMaxHeight() else Modifier.verticalScroll(screenScroll)).padding(horizontal = 16.dp, vertical = 16.dp),
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
                        TextButton(onClick = { showLicenses = true }) { Text(stringResource(R.string.third_party_licenses)) }
                        if (BuildConfig.DEBUG) {
                            TextButton(onClick = { showDiagnostics = true }) { Text(stringResource(R.string.startup_diagnostics)) }
                            TextButton(onClick = { showSelfCheck = true }) { Text(stringResource(R.string.self_check_title)) }
                        }
                    }
                    AlphaNotice()
                    if (showSelfCheck && BuildConfig.DEBUG) SelfCheckDialog { showSelfCheck = false }
                    if (showDiagnostics && BuildConfig.DEBUG) {
                        val events by StartupDiagnostics.events.collectAsState()
                        AlertDialog(onDismissRequest = { showDiagnostics = false },
                            title = { Text(stringResource(R.string.startup_diagnostics)) },
                            text = { Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.startup_diagnostics_hint))
                                Text(events.joinToString("\n"), style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.testTag("startup-diagnostics"))
                            } },
                            confirmButton = { TextButton(onClick = { showDiagnostics = false }) { Text(stringResource(R.string.back)) } })
                    }
                    if (showLicenses) {
                        val context = LocalContext.current
                        val license = remember(context) {
                            context.resources.openRawResource(R.raw.sqlcipher_license).bufferedReader().use { it.readText() }
                        }
                        AlertDialog(onDismissRequest = { showLicenses = false },
                            title = { Text(stringResource(R.string.third_party_licenses)) },
                            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.sqlcipher_license_explanation))
                                Text(license)
                            } },
                            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.back)) } })
                    }
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
                            onDismiss = { dialog = "" }, onSelect = { dialog = ""; pendingLanguage = AppLanguage.entries[it] })
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
                    if (!obligations) {
                        BrandHeader(dark)
                        Text(stringResource(R.string.tagline), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    when (storageState) {
                        StorageState.Loading -> {
                            CircularProgressIndicator(Modifier.size(28.dp))
                            Text(stringResource(R.string.storage_loading))
                        }
                        StorageState.Failed -> {
                            Text(stringResource(R.string.storage_unavailable), color = colors.error,
                                modifier = Modifier.testTag("storage-error"))
                            Button(onClick = onRetryStorage) { Text(stringResource(R.string.storage_retry)) }
                        }
                        is StorageState.Ready -> {
                            if (obligations) {
                                holder.SaveableStateProvider("credit-workspace") {
                                    ObligationPreview(storageState.records,
                                        saveRecord = { storageState.repository.save(it) },
                                        deleteRecord = { check(storageState.repository.delete(it)) },
                                        payments = storageState.payments, route = route,
                                        onRoute = { route = it }, paymentRequested = paymentRequested,
                                        onPaymentRequested = { paymentRequested = it }, scroll = screenScroll)
                                }
                            } else {
                                PreviewDashboard(storageState.records,
                                    open = { openRoute("list") },
                                    add = { openRoute("new") },
                                    openCredit = { openRoute("detail:" + it) },
                                    recordPayment = { openRoute("detail:" + it, true) })
                            }
                        }
                    }
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
        Appearance.SYSTEM -> stringResource(R.string.system_value, stringResource(R.string.system_short),
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
        AppLanguage.SYSTEM -> stringResource(R.string.system_language_value, systemName)
        AppLanguage.RUSSIAN -> stringResource(R.string.language_ru)
        AppLanguage.ENGLISH -> stringResource(R.string.language_en)
    }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            PreferenceRow(R.string.theme_label, theme, R.drawable.ic_theme, onTheme)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceRow(R.string.language_short, languageValue, R.drawable.ic_language, onLanguage)
        }
    }
}

@Composable
private fun PreferenceRow(label: Int, value: String, icon: Int, action: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = action)
        .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_chevron), null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
