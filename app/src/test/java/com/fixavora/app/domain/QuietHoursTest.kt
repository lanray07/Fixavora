package com.fixavora.app.domain

import com.fixavora.app.notifications.QuietHours
import org.junit.Assert.*
import org.junit.Test

class QuietHoursTest {
    @Test fun midnightWrapAndEndBoundary() {
        assertTrue(QuietHours.contains(22,22,8)); assertTrue(QuietHours.contains(0,22,8))
        assertTrue(QuietHours.contains(7,22,8)); assertFalse(QuietHours.contains(8,22,8))
        assertFalse(QuietHours.contains(21,22,8))
    }
    @Test fun daytimeWindowAndDisabledWindow() {
        assertFalse(QuietHours.contains(8,9,17)); assertTrue(QuietHours.contains(9,9,17))
        assertFalse(QuietHours.contains(17,9,17))
        (0..23).forEach { assertFalse(QuietHours.contains(it,0,0)) }
    }
}
