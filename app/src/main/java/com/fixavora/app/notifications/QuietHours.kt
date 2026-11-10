package com.fixavora.app.notifications

object QuietHours {
    /** Equal endpoints disable quiet hours rather than silencing every notification. */
    fun contains(hour: Int, start: Int, end: Int): Boolean {
        require(hour in 0..23 && start in 0..23 && end in 0..23)
        return when {
            start == end -> false
            start < end -> hour in start until end
            else -> hour >= start || hour < end
        }
    }
}
