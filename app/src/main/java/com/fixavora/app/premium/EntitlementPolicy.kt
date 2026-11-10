package com.fixavora.app.premium

/** A bounded offline grace period; an account mismatch or authoritative revocation wins. */
object EntitlementPolicy {
    const val MAX_OFFLINE_AGE_MS = 7L * 24 * 60 * 60 * 1000
    fun cached(active: Boolean, cachedUser: String?, currentUser: String?, verifiedAt: Long, now: Long): Boolean =
        active && !currentUser.isNullOrBlank() && cachedUser == currentUser &&
            verifiedAt > 0 && now >= verifiedAt && now - verifiedAt <= MAX_OFFLINE_AGE_MS
}
