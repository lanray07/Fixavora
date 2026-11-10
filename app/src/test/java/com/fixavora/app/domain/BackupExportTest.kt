package com.fixavora.app.domain

import com.fixavora.app.backup.*
import com.fixavora.app.data.*
import com.fixavora.app.reports.Exports
import org.junit.Assert.*
import org.junit.Test

class BackupExportTest {
    private val password = "A strong portable passphrase".toCharArray()
    private val data = BackupData(records = listOf(HomeRecord(kind = "PROPERTY", title = "Home")), events = emptyList())
    @Test fun encryptedBackupRoundTripsAndUsesRandomNonce() {
        val first = EncryptedBackup.encode(data, password)
        val second = EncryptedBackup.encode(data, password)
        assertFalse(first.contentEquals(second))
        assertFalse(String(first).contains("Home"))
        assertEquals(data, EncryptedBackup.decode(first, password))
    }
    @Test fun tamperingAndWrongPassphraseFail() {
        val bytes = EncryptedBackup.encode(data, password)
        assertTrue(runCatching { EncryptedBackup.decode(bytes, "A different passphrase".toCharArray()) }.isFailure)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertTrue(runCatching { EncryptedBackup.decode(bytes, password) }.isFailure)
    }
    @Test fun truncatedAndUnknownBackupFail() {
        assertTrue(runCatching { EncryptedBackup.decode(byteArrayOf(1,2,3), password) }.isFailure)
    }
    @Test fun csvEscapesQuotesNewlinesAndFormulaInjection() {
        assertEquals("\"a\"\"b\nc\"", Exports.csvCell("a\"b\nc"))
        assertEquals("\"'=SUM(A1)\"", Exports.csvCell("=SUM(A1)"))
        assertEquals("\"'  @IMPORT()\"", Exports.csvCell("  @IMPORT()"))
    }
}
