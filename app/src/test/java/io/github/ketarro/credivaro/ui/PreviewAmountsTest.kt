package io.github.ketarro.credivaro.ui
import org.junit.Assert.*
import org.junit.Test
import java.util.Currency
import io.github.ketarro.credivaro.model.Money

class PreviewAmountsTest {
    private val rub = Currency.getInstance("RUB")
    @Test fun unknownIsNotZero() { assertNull(parsePreviewMoney("", rub)); assertEquals(Money("RUB",0), parsePreviewMoney("0",rub)) }
    @Test fun decimalComma() { assertEquals(Money("RUB",12345), parsePreviewMoney("123,45",rub)) }
    @Test fun decimalPoint() { assertEquals(Money("RUB",12345), parsePreviewMoney("123.45",rub)) }
    @Test(expected=IllegalArgumentException::class) fun rejectsExtraPrecision() { parsePreviewMoney("1.001",rub) }
    @Test(expected=IllegalArgumentException::class) fun rejectsNegative() { parsePreviewMoney("-1",rub) }
    @Test(expected=ArithmeticException::class) fun rejectsOverflow() { parsePreviewMoney("999999999999999999999999",rub) }
}
