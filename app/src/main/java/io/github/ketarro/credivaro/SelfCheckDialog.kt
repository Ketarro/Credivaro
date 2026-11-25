package io.github.ketarro.credivaro

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SelfCheckDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val report by SelfCheckSession.state.collectAsState()
    val names = listOf(R.string.self_fin01, R.string.self_fin02, R.string.self_fin03, R.string.self_fin04, R.string.self_fin05, R.string.self_fin06, R.string.self_fin07, R.string.self_fin08, R.string.self_fin09, R.string.self_fin10, R.string.self_db01, R.string.self_db02, R.string.self_db03, R.string.self_db04, R.string.self_db05, R.string.self_db06, R.string.self_db07, R.string.self_db08, R.string.self_pay01, R.string.self_pay02, R.string.self_pay03, R.string.self_pay04, R.string.self_pay05, R.string.self_mig01, R.string.self_mig02, R.string.self_mig03)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.self_check_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.self_check_hint))
                Text(stringResource(R.string.self_check_count, report.results.count { it.passed }, SelfCheckSession.ids.size),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("self-check-count"))
                if (report.running) LinearProgressIndicator(progress = { report.results.size.toFloat() / SelfCheckSession.ids.size },
                    modifier = Modifier.fillMaxWidth())
                Text(stringResource(when {
                    report.running -> R.string.self_check_running
                    report.aborted -> R.string.self_check_aborted
                    report.complete -> R.string.self_check_finished
                    else -> R.string.self_check_idle
                }))
                SelfCheckSession.ids.forEachIndexed { index, id ->
                    val result = report.results.firstOrNull { it.id == id }
                    val status = when {
                        result == null -> stringResource(R.string.self_check_pending)
                        result.passed -> stringResource(R.string.self_check_pass)
                        else -> stringResource(R.string.self_check_fail)
                    }
                    Text("$id · ${stringResource(names[index])}\n$status" +
                        (result?.let { " · ${it.ms} ms" } ?: ""),
                        color = if (result?.passed == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (!report.running && report.results.isNotEmpty()) TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Credivaro self-check", SelfCheckSession.reportText(report)))
                }) { Text(stringResource(R.string.self_check_copy)) }
            }
        },
        confirmButton = { Button(onClick = { SelfCheckSession.start(context) }, enabled = !report.running,
            modifier = Modifier.testTag("self-check-start")) { Text(stringResource(R.string.self_check_start)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.back)) } }
    )
}
