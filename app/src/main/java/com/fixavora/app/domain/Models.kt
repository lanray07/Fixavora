package com.fixavora.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.UUID

enum class Kind { PROPERTY, ROOM, TASK, REPAIR, APPLIANCE, WARRANTY, CONTRACTOR, EXPENSE, APPOINTMENT }
enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM_DAYS }
enum class RepairStatus { REPORTED, PLANNED, IN_PROGRESS, WAITING_PARTS, COMPLETED, CANCELLED }

/** The anchor is retained across occurrences: Jan 31 -> Feb 28 -> Mar 31. */
object Recurrence {
    fun next(due: LocalDate, repeat: Repeat, interval: Int, anchorDay: Int): LocalDate? {
        require(interval in 1..3650)
        require(anchorDay in 1..31)
        fun months(n: Long): LocalDate {
            val month = YearMonth.from(due).plusMonths(n)
            return month.atDay(anchorDay.coerceAtMost(month.lengthOfMonth()))
        }
        return when (repeat) {
            Repeat.NONE -> null
            Repeat.DAILY -> due.plusDays(1)
            Repeat.WEEKLY -> due.plusWeeks(1)
            Repeat.MONTHLY -> months(1)
            Repeat.QUARTERLY -> months(3)
            Repeat.YEARLY -> months(12)
            Repeat.CUSTOM_DAYS -> due.plusDays(interval.toLong())
        }
    }
}

object Money {
    fun parse(input: String, currency: String): Long {
        val scale = Currency.getInstance(currency).defaultFractionDigits
        require(scale >= 0)
        val decimal = BigDecimal(input.trim())
        require(decimal.signum() >= 0)
        return decimal.setScale(scale, RoundingMode.UNNECESSARY).movePointRight(scale).longValueExact()
    }
    fun decimal(minor: Long, currency: String): BigDecimal =
        BigDecimal.valueOf(minor, Currency.getInstance(currency).defaultFractionDigits)
}

data class ProposedAction(val title: String, val category: String, val due: LocalDate?, val repeat: Repeat, val hazardous: Boolean)

/** Offline organisation only; never provides repair procedures or silently changes records. */
object LocalAssistant {
    private val hazards = listOf("gas", "electri", "structur", "boiler", "smoke", "fire", "gaz", "électri", "caldera", "strom", "غاز", "बिजली", "ガス")
    fun propose(text: String, today: LocalDate): ProposedAction? {
        val input = text.trim().take(500)
        if (input.isBlank()) return null
        val lower = input.lowercase()
        val category = when {
            listOf("tap", "leak", "plumb", "robinet", "fuite", "grifo", "wasser", "水", "تسرب").any(lower::contains) -> "plumbing"
            hazards.any(lower::contains) -> "safety"
            listOf("garden", "jardin", "garten").any(lower::contains) -> "garden"
            else -> "general"
        }
        val repeat = if (listOf("yearly", "annual", "annuel", "anual", "jährlich").any(lower::contains)) Repeat.YEARLY else Repeat.NONE
        val due = when {
            listOf("tomorrow", "demain", "mañana", "morgen").any(lower::contains) -> today.plusDays(1)
            listOf("next month", "mois prochain", "próximo mes", "nächsten monat").any(lower::contains) -> today.plusMonths(1)
            else -> null
        }
        return ProposedAction(input, category, due, repeat, hazards.any(lower::contains))
    }
    fun duplicates(title: String, existing: List<String>): List<String> {
        fun words(s: String) = s.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val a = words(title)
        return existing.filter { val b = words(it); a.isNotEmpty() && a.intersect(b).size.toDouble() / a.union(b).size >= 0.6 }
    }
}
