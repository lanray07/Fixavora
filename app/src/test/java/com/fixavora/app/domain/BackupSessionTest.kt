package com.fixavora.app.domain

import com.fixavora.app.backup.BackupSession
import org.junit.Assert.*
import org.junit.Test

class BackupSessionTest {
    @Test fun passphraseIsSingleUseAndCancellationRemovesIt() {
        val session=BackupSession()
        session.begin("A disposable test passphrase")
        val taken=session.take()!!
        assertEquals("A disposable test passphrase",String(taken))
        assertNull(session.take());taken.fill('\u0000')
        session.begin("First disposable passphrase")
        session.begin("Replacement disposable passphrase")
        assertEquals("Replacement disposable passphrase",String(session.take()!!))
        session.begin("Cancelled disposable passphrase");session.clear()
        assertNull(session.take())
    }
}
