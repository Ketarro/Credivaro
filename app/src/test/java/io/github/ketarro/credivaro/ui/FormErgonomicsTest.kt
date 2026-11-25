package io.github.ketarro.credivaro.ui

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Test
import java.util.Currency

class FormErgonomicsTest {
    @Test fun groupingDoesNotChangeRawOrRound() {
        val raw = "1234567,89"
        val output = GroupedDecimal.filter(AnnotatedString(raw))
        assertEquals("1\u00A0234\u00A0567,89", output.text.text)
        for (i in 0..raw.length) assertEquals(i, output.offsetMapping.transformedToOriginal(output.offsetMapping.originalToTransformed(i)))
        assertEquals("123.456", GroupedDecimal.filter(AnnotatedString("123.456")).text.text)
        assertEquals("bad", GroupedDecimal.filter(AnnotatedString("bad")).text.text)
    }
    @Test fun fieldErrorsAndSavedLenders() {
        val rub = Currency.getInstance("RUB")
        assertFalse(invalidAmount("", rub))
        assertTrue(invalidAmount("", rub, true))
        assertTrue(invalidAmount("0", rub, true))
        assertTrue(invalidAmount("1.001", rub))
        assertTrue(invalidAmount("-1", rub))
        assertFalse(invalidAmount("1234567,89", rub, true))
        assertEquals(listOf("Bank A", "Bank B"), savedLenders(listOf("Bank B", "Bank A", "bank a", null, " ")))
    }
}
