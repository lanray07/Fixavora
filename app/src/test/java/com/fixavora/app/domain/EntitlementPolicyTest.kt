package com.fixavora.app.domain

import com.fixavora.app.premium.EntitlementPolicy
import org.junit.Assert.*
import org.junit.Test

class EntitlementPolicyTest {
    @Test fun offlineAccessRequiresConfirmedAccountAndRecentVerification() {
        val verified = 1_000_000L
        assertTrue(EntitlementPolicy.cached(true, "A", "A", verified, verified + 1))
        assertFalse(EntitlementPolicy.cached(true, "A", null, verified, verified + 1))
        assertFalse(EntitlementPolicy.cached(true, "A", "B", verified, verified + 1))
        assertFalse(EntitlementPolicy.cached(true, "A", "A", verified, verified + EntitlementPolicy.MAX_OFFLINE_AGE_MS + 1))
        assertFalse(EntitlementPolicy.cached(true, "A", "A", verified, verified - 1))
        assertFalse(EntitlementPolicy.cached(true, "A", "A", 0, verified))
        assertFalse(EntitlementPolicy.cached(false, "A", "A", verified, verified))
    }
}
