package com.fixavora.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fixavora.app.data.*
import com.fixavora.app.backup.*
import com.fixavora.app.reports.Exports
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private lateinit var db: HomeDatabase
    private lateinit var repo: HomeRepository
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, HomeDatabase::class.java).build(); repo = HomeRepository(db) }
    @After fun close() { db.close() }
    @Test fun lifecycleRecurrenceUndoAndCascade() = runBlocking {
        val property = HomeRecord(kind = "PROPERTY", title = "Test Home")
        repo.save(property, false)
        val room = HomeRecord(kind = "ROOM", title = "Kitchen", propertyId = property.id)
        repo.save(room, false)
        val task = HomeRecord(kind = "TASK", title = "Clean filter", propertyId = property.id, roomId = room.id, due = "2026-01-31", repeat = "MONTHLY", anchorDay = 31)
        repo.save(task, false)
        repo.complete(task.id)
        assertEquals("2026-02-28", repo.dao.find(task.id)!!.due)
        repo.undoCompletion(task.id)
        assertEquals("2026-01-31", repo.dao.find(task.id)!!.due)
        repo.snooze(task.id)
        assertEquals("2026-01-31", repo.dao.find(task.id)!!.due)
        repo.dao.attach(Attachment(recordId = task.id, uri = "content://test/document", name = "receipt.pdf", mime = "application/pdf"))
        repo.delete(room.id)
        assertNull(repo.dao.find(task.id)!!.roomId)
        repo.delete(property.id)
        assertTrue(repo.dao.all().isEmpty()); assertTrue(repo.dao.events().isEmpty()); assertTrue(repo.dao.attachments().isEmpty())
    }
    @Test fun freeLimitsAndExistingRecordAccess() = runBlocking {
        val property = HomeRecord(kind = "PROPERTY", title = "Home")
        repo.save(property, false)
        repeat(15) { repo.save(HomeRecord(kind = "TASK", title = "Task $it", propertyId = property.id), false) }
        assertTrue(runCatching { repo.save(HomeRecord(kind = "TASK", title = "Too many", propertyId = property.id), false) }.exceptionOrNull() is RuleViolation)
        assertTrue(runCatching { repo.save(HomeRecord(kind = "PROPERTY", title = "Second"), false) }.isFailure)
        val extra = HomeRecord(kind = "TASK", title = "Premium record", propertyId = property.id)
        repo.save(extra, true)
        repo.save(extra.copy(notes = "Still accessible after entitlement unavailable"), false)
        assertEquals(17, repo.dao.all().size)
    }
    @Test fun invalidCrossPropertyLinkRollsBack() = runBlocking {
        val a = HomeRecord(kind = "PROPERTY", title = "A"); val b = HomeRecord(kind = "PROPERTY", title = "B")
        repo.save(a, true); repo.save(b, true)
        val room = HomeRecord(kind = "ROOM", title = "Kitchen", propertyId = b.id); repo.save(room, true)
        assertTrue(runCatching { repo.save(HomeRecord(kind = "TASK", title = "Wrong room", propertyId = a.id, roomId = room.id), true) }.isFailure)
        assertEquals(3, repo.dao.all().size)
    }
    @Test fun encryptedRestoreIsAtomic() = runBlocking {
        val p = HomeRecord(kind = "PROPERTY", title = "Keep me"); repo.save(p, false)
        val bad = BackupData(records = listOf(HomeRecord(kind = "TASK", title = "Orphan", propertyId = "missing")), events = emptyList())
        assertTrue(runCatching { EncryptedBackup.restore(repo, bad) }.isFailure)
        assertEquals("Keep me", repo.dao.all().single().title)
        val pass = "A long test passphrase".toCharArray()
        val data = BackupData(records = repo.dao.all(), events = repo.dao.events())
        EncryptedBackup.restore(repo, EncryptedBackup.decode(EncryptedBackup.encode(data, pass), pass))
        assertEquals("Keep me", repo.dao.all().single().title)
    }
    @Test fun reportsContainRealData() = runBlocking {
        val p = HomeRecord(kind = "PROPERTY", title = "Fictional Home")
        val e = HomeRecord(kind = "EXPENSE", title = "Tap repair", propertyId = p.id, amountMinor = 4999, currency = "GBP")
        repo.save(p, true); repo.save(e, true)
        val csv = ByteArrayOutputStream(); Exports.csv(context, repo.dao.all(), csv)
        assertTrue(csv.toString("UTF-8").contains("49.99")); assertTrue(csv.toString("UTF-8").contains("Tap repair"))
        val pdf = ByteArrayOutputStream(); Exports.pdf(Exports.summary(context, repo.dao.all(), java.util.Locale.UK, "All dates"), pdf)
        assertTrue(pdf.toByteArray().copyOfRange(0, 4).contentEquals("%PDF".toByteArray()))
    }
    @Test fun dataSurvivesDatabaseReopen() = runBlocking {
        val name = "restart-test-${System.nanoTime()}.db"
        var disk = Room.databaseBuilder(context, HomeDatabase::class.java, name).build()
        try {
            disk.records().put(HomeRecord(kind = "PROPERTY", title = "Persistent Home")); disk.close()
            disk = Room.databaseBuilder(context, HomeDatabase::class.java, name).build()
            assertEquals("Persistent Home", disk.records().all().single().title)
        } finally { disk.close(); context.deleteDatabase(name) }
    }
    @Test fun migrationPreservesRecordsAndRebuildsIndex() = runBlocking {
        val name = "migration-test-${System.nanoTime()}.db"
        var disk = Room.databaseBuilder(context, HomeDatabase::class.java, name).build()
        try {
            disk.records().put(HomeRecord(kind = "PROPERTY", title = "Before migration"))
            disk.openHelper.writableDatabase.execSQL("DROP INDEX index_records_kind_due")
            disk.openHelper.writableDatabase.execSQL("PRAGMA user_version = 1")
            disk.close()
            disk = Room.databaseBuilder(context, HomeDatabase::class.java, name).addMigrations(HomeDatabase.MIGRATION_1_2).build()
            assertEquals("Before migration", disk.records().all().single().title)
            disk.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_records_kind_due'").use { assertTrue(it.moveToFirst()) }
        } finally { disk.close(); context.deleteDatabase(name) }
    }
}
