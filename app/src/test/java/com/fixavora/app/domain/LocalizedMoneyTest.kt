package com.fixavora.app.domain

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class LocalizedMoneyTest {
    @Test fun selectedCurrencyPrecisionSurvivesDifferentLocaleDefaults() {
        assertTrue(Money.formatter("GBP",Locale.JAPAN).format(Money.decimal(11350,"GBP")).endsWith("113.50"))
        assertTrue(Money.formatter("GBP",Locale.KOREA).format(Money.decimal(11350,"GBP")).endsWith("113.50"))
        assertTrue(Money.formatter("JPY",Locale.UK).format(Money.decimal(50,"JPY")).endsWith("50"))
        assertTrue(Money.formatter("KWD",Locale.US).format(Money.decimal(1234,"KWD")).endsWith("1.234"))
    }
    @Test fun decimalSeparatorsAndNativeDigitsKeepExactMinorUnits() {
        assertEquals(4999L,Money.parse("49,99","EUR",Locale.FRANCE))
        assertEquals(4999L,Money.parse("49.99","EUR",Locale.FRANCE))
        assertEquals(4999L,Money.parse("٤٩٫٩٩","SAR",Locale.forLanguageTag("ar")))
        assertEquals(50L,Money.parse("５０","JPY",Locale.JAPAN))
        assertEquals("2026-11-30",normalizeDigits("٢٠٢٦-١١-٣٠"))
    }
    @Test fun fractionalWholeUnitCurrenciesAndAmbiguousGroupingAreRejected() {
        assertTrue(runCatching { Money.parse("1,5","JPY",Locale.FRANCE) }.isFailure)
        assertTrue(runCatching { Money.parse("1.234,56","EUR",Locale.FRANCE) }.isFailure)
        assertTrue(runCatching { Money.parse("-1,00","EUR",Locale.FRANCE) }.isFailure)
    }
}
