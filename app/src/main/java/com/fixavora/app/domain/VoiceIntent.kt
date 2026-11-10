package com.fixavora.app.domain

enum class VoiceIntent { DUE_WEEK, OVERDUE, EXPENSES, WARRANTIES, COMPLETE, APPOINTMENT, CREATE }
object VoiceIntents {
    fun parse(text: String, dictionary: Map<String, List<String>>): VoiceIntent {
        val lower = text.lowercase().trim()
        return VoiceIntent.entries.firstOrNull { intent ->
            intent != VoiceIntent.CREATE && dictionary[intent.name].orEmpty().any { lower.contains(it.lowercase()) }
        } ?: VoiceIntent.CREATE
    }
}
