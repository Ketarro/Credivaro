package io.github.ketarro.credivaro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ketarro.credivaro.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Material DatePicker uses UTC midnight, not the device's local midnight.
internal fun pickerMillis(date: LocalDate): Long = date.toEpochDay() * 86_400_000L
internal fun pickerDate(millis: Long): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(millis, 86_400_000L))
internal fun dateAllowed(date: LocalDate, min: LocalDate?, max: LocalDate?): Boolean =
    (min == null || !date.isBefore(min)) && (max == null || !date.isAfter(max))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarField(value: String, update: (String) -> Unit, label: Int, tag: String,
    today: LocalDate, min: LocalDate? = null, max: LocalDate? = null,
    optional: Boolean = false, enabled: Boolean = true, shortcuts: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    val current = value.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val locale = LocalConfiguration.current.locales[0]
    val formatted = current?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    OutlinedButton(onClick = { focus.clearFocus(force = true); keyboard?.hide(); open = true },
        enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) {
        Text(formatted ?: stringResource(R.string.ux_choose_date), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.ux_calendar))
    }
    if (optional && current != null) TextButton(onClick = { update("") }, enabled = enabled,
        modifier = Modifier.testTag(tag + "-clear")) { Text(stringResource(R.string.ux_clear_date)) }
    if (shortcuts) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { update(today.toString()) }, enabled = enabled && dateAllowed(today, min, max),
            modifier = Modifier.testTag(tag + "-today")) { Text(stringResource(R.string.ux_today)) }
        TextButton(onClick = { update(today.minusDays(1).toString()) }, enabled = enabled && dateAllowed(today.minusDays(1), min, max),
            modifier = Modifier.testTag(tag + "-yesterday")) { Text(stringResource(R.string.ux_yesterday)) }
    }
    if (open) {
        val selected = current?.takeIf { dateAllowed(it, min, max) }
        val initial = selected ?: today.coerceIn(min ?: LocalDate.of(1900, 1, 1), max ?: LocalDate.of(2200, 12, 31))
        val years = minOf(1900, initial.year, min?.year ?: initial.year)..maxOf(2200, initial.year, max?.year ?: initial.year)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selected?.let(::pickerMillis),
            initialDisplayedMonthMillis = pickerMillis(initial),
            yearRange = years,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = dateAllowed(pickerDate(utcTimeMillis), min, max)
                override fun isSelectableYear(year: Int) = (min == null || year >= min.year) && (max == null || year <= max.year)
            })
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(enabled = state.selectedDateMillis?.let { dateAllowed(pickerDate(it), min, max) } == true,
                modifier = Modifier.testTag(tag + "-confirm"), onClick = {
                    state.selectedDateMillis?.let { update(pickerDate(it).toString()) }
                    open = false
                }) { Text(stringResource(R.string.ux_select_date)) } },
            dismissButton = { TextButton(onClick = { open = false }, modifier = Modifier.testTag(tag + "-dismiss")) {
                Text(stringResource(R.string.cancel))
            } }) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}
