package com.fixavora.app.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fixavora.app.domain.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "records", foreignKeys = [
    ForeignKey(entity = HomeRecord::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = HomeRecord::class, parentColumns = ["id"], childColumns = ["roomId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = HomeRecord::class, parentColumns = ["id"], childColumns = ["contractorId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = HomeRecord::class, parentColumns = ["id"], childColumns = ["relatedId"], onDelete = ForeignKey.SET_NULL)
], indices = [Index("propertyId"), Index("roomId"), Index("contractorId"), Index("relatedId"), Index(value = ["kind", "due"])])
data class HomeRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kind: String,
    val title: String,
    val propertyId: String? = null,
    val roomId: String? = null,
    val contractorId: String? = null,
    val relatedId: String? = null,
    val due: String? = null,
    val date: String = LocalDate.now().toString(),
    val priority: String = "normal",
    val status: String = "REPORTED",
    val done: Boolean = false,
    val archived: Boolean = false,
    val repeat: String = "NONE",
    val interval: Int = 1,
    val anchorDay: Int = 1,
    val paused: Boolean = false,
    val amountMinor: Long = 0,
    val currency: String = "GBP",
    val category: String = "general",
    val notes: String = "",
    val fields: String = "{}",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "events", foreignKeys = [ForeignKey(entity = HomeRecord::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordId")])
data class HistoryEvent(@PrimaryKey val id: String = UUID.randomUUID().toString(), val recordId: String, val action: String, val at: Long = System.currentTimeMillis(), val snapshot: String)

@Entity(tableName = "attachments", foreignKeys = [ForeignKey(entity = HomeRecord::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)], indices = [Index("recordId")])
data class Attachment(@PrimaryKey val id: String = UUID.randomUUID().toString(), val recordId: String, val uri: String, val name: String, val mime: String)

@Dao
interface HomeDao {
    @Query("SELECT * FROM records ORDER BY updatedAt DESC") fun observe(): Flow<List<HomeRecord>>
    @Query("SELECT * FROM records") suspend fun all(): List<HomeRecord>
    @Query("SELECT * FROM records WHERE id=:id") suspend fun find(id: String): HomeRecord?
    @Upsert suspend fun put(record: HomeRecord)
    @Query("DELETE FROM records WHERE id=:id") suspend fun delete(id: String)
    @Query("DELETE FROM records") suspend fun clear()
    @Insert suspend fun event(event: HistoryEvent)
    @Query("SELECT * FROM events WHERE recordId=:id ORDER BY at DESC") fun history(id: String): Flow<List<HistoryEvent>>
    @Query("SELECT * FROM events ORDER BY at DESC") suspend fun events(): List<HistoryEvent>
    @Insert suspend fun attach(attachment: Attachment)
    @Query("SELECT * FROM attachments WHERE recordId=:id") fun attachments(id: String): Flow<List<Attachment>>
    @Query("SELECT * FROM attachments") suspend fun attachments(): List<Attachment>
    @Query("DELETE FROM attachments WHERE id=:id") suspend fun detach(id: String)
}

@Database(entities = [HomeRecord::class, HistoryEvent::class, Attachment::class], version = 2, exportSchema = true)
abstract class HomeDatabase : RoomDatabase() {
    abstract fun records(): HomeDao
    companion object {
        // Version 1 used the same tables without the scheduling index. No destructive fallback.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE INDEX IF NOT EXISTS index_records_kind_due ON records(kind, due)") }
        }
    }
}
