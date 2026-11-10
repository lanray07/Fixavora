package com.fixavora.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DomainTest {
    @Test fun monthEndDoesNotDrift() {
        val feb = Recurrence.next(LocalDate.of(2025, 1, 31), Repeat.MONTHLY, 1, 31)!!
        assertEquals(LocalDate.of(2025, 2, 28), feb)
        assertEquals(LocalDate.of(2025, 3, 31), Recurrence.next(feb, Repeat.MONTHLY, 1, 31))
    }
    @Test fun leapYearAnchorReturns() {
        val date = LocalDate.of(2024, 2, 29)
        var next = date
        repeat(4) { next = Recurrence.next(next, Repeat.YEARLY, 1, 29)!! }
        assertEquals(LocalDate.of(2028, 2, 29), next)
    }
    @Test fun daylightSavingsKeepsCalendarDate() {
        val due = LocalDate.of(2026, 3, 28)
        val next = Recurrence.next(due, Repeat.DAILY, 1, 28)!!
        assertEquals(LocalDate.of(2026, 3, 29), next)
        assertEquals(23, Duration.between(next.atStartOfDay(ZoneId.of("Europe/London")), next.plusDays(1).atStartOfDay(ZoneId.of("Europe/London"))).toHours().toInt())
    }
    @Test fun quarterlyAndCustom() {
        assertEquals(LocalDate.of(2027, 2, 28), Recurrence.next(LocalDate.of(2026, 11, 30), Repeat.QUARTERLY, 1, 30))
        assertEquals(LocalDate.of(2026, 11, 17), Recurrence.next(LocalDate.of(2026, 11, 10), Repeat.CUSTOM_DAYS, 7, 10))
        assertNull(Recurrence.next(LocalDate.now(), Repeat.NONE, 1, 1))
    }
    @Test fun moneyIsExactAndCurrencyAware() {
        assertEquals(499, Money.parse("4.99", "GBP"))
        assertEquals(499, Money.parse("499", "JPY"))
        assertEquals(1234, Money.parse("1.234", "KWD"))
    }
    @Test fun moneyRejectsRoundingNegativeAndOverflow() {
        listOf("1.001", "-1", "99999999999999999999999", "NaN").forEach { value -> assertTrue(runCatching { Money.parse(value, "GBP") }.isFailure) }
    }
    @Test fun localAssistantNeverSavesAndFlagsHazards() {
        val action = LocalAssistant.propose("Service the boiler next month", LocalDate.of(2026, 11, 10))!!
        assertTrue(action.hazardous)
        assertEquals(LocalDate.of(2026, 12, 10), action.due)
        assertEquals("safety", action.category)
        assertNull(LocalAssistant.propose(" ", LocalDate.now()))
    }
    @Test fun multilingualDateDraft() {
        val action = LocalAssistant.propose("Réparer le robinet demain", LocalDate.of(2026, 11, 10))!!
        assertEquals("plumbing", action.category)
        assertEquals(LocalDate.of(2026, 11, 11), action.due)
    }
    @Test fun duplicatesAreWordBased() {
        assertEquals(listOf("Clean kitchen tap"), LocalAssistant.duplicates("clean kitchen tap", listOf("Clean kitchen tap", "Boiler service")))
    }
    @Test fun commandDictionariesRouteReadOnlyAndMutatingIntents() {
        val dictionary = mapOf("DUE_WEEK" to listOf("cette semaine"), "COMPLETE" to listOf("terminer"))
        assertEquals(VoiceIntent.DUE_WEEK, VoiceIntents.parse("Que faire cette semaine ?", dictionary))
        assertEquals(VoiceIntent.COMPLETE, VoiceIntents.parse("Terminer Nettoyer le filtre", dictionary))
        assertEquals(VoiceIntent.CREATE, VoiceIntents.parse("Un robinet fuit", dictionary))
    }
}
