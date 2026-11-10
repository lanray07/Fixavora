package com.fixavora.app.backup

/** In-memory handoff across the system picker and Activity recreation.
 * Never persist a backup passphrase in saved instance state or on disk. */
class BackupSession {
    private var pending: CharArray? = null
    fun begin(passphrase: String) { clear(); pending = passphrase.toCharArray() }
    fun take(): CharArray? = pending.also { pending = null }
    fun clear() { pending?.fill('\u0000'); pending = null }
}
