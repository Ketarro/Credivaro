package io.github.ketarro.credivaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ketarro.credivaro.ui.CredivaroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var theme by rememberSaveable { mutableIntStateOf(0) }
            val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CredivaroTheme(dark) { AlphaScreen(theme, { theme = it }) }
        }
    }
}

@Composable
private fun AlphaScreen(theme: Int, onThemeChange: (Int) -> Unit) {
    var about by rememberSaveable { mutableStateOf(false) }
    BackHandler(about) { about = false }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = !about, onClick = { about = false },
                    icon = { Text("⌂") }, label = { Text(stringResource(R.string.home)) })
                NavigationBarItem(selected = about, onClick = { about = true },
                    icon = { Text("ⓘ") }, label = { Text(stringResource(R.string.about)) })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            }
            if (!about) {
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.alpha_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.alpha_description))
                    }
                }
                Text(stringResource(R.string.alpha_warning), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { about = true }) { Text(stringResource(R.string.about)) }
            } else {
                Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME))
                Text(stringResource(R.string.license_label))
                Text(stringResource(R.string.privacy_description))
                Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.titleMedium)
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { index, title ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == index, onClick = { onThemeChange(index) })
                        Text(stringResource(title))
                    }
                }
                Text(stringResource(R.string.alpha_warning), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
