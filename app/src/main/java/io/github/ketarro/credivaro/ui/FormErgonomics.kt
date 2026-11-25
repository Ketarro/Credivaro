package io.github.ketarro.credivaro.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Currency

/** Display-only grouping; raw input and exact minor units are never rounded or converted. */
internal object GroupedDecimal : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (!raw.matches(Regex("[0-9]+([.,][0-9]*)?"))) return TransformedText(text, OffsetMapping.Identity)
        val integerLength = raw.indexOfFirst { it == '.' || it == ',' }.let { if (it < 0) raw.length else it }
        val forward = IntArray(raw.length + 1)
        val back = mutableListOf(0)
        val result = StringBuilder()
        raw.forEachIndexed { index, char ->
            if (index > 0 && index < integerLength && (integerLength - index) % 3 == 0) {
                result.append('\u00A0')
                back.add(index)
            }
            result.append(char)
            back.add(index + 1)
            forward[index + 1] = result.length
        }
        return TransformedText(AnnotatedString(result.toString()), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = forward[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int): Int = back[offset.coerceIn(0, result.length)]
        })
    }
}
internal fun invalidAmount(value: String, currency: Currency, required: Boolean = false): Boolean =
    if (value.isBlank()) required else runCatching {
        val amount = requireNotNull(parsePreviewMoney(value, currency))
        if (required) require(amount.minorUnits > 0)
    }.isFailure
internal fun invalidRate(value: String): Boolean =
    value.isNotBlank() && runCatching { parsePreviewRate(value) }.isFailure
internal fun savedLenders(values: List<String?>): List<String> =
    values.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinctBy { it.lowercase(java.util.Locale.ROOT) }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
