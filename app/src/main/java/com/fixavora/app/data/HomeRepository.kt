package com.fixavora.app.data

import androidx.room.withTransaction
import com.fixavora.app.domain.*
import com.google.gson.Gson
import java.time.LocalDate

class RuleViolation(val key: String) : IllegalArgumentException(key)

class HomeRepository(val database: HomeDatabase) {
    val dao = database.records()
    private val gson = Gson()

    suspend fun save(record: HomeRecord, premium: Boolean) = database.withTransaction {
        val all = dao.all()
        val old = all.find { it.id == record.id }
        validate(record, all)
        if (!premium) {
            if (record.kind == "PROPERTY" && !record.archived && (old == null || old.archived) && all.count { it.id != record.id && it.kind == "PROPERTY" && !it.archived } >= 1) throw RuleViolation("premium_required")
            if (record.kind == "TASK" && !record.done && !record.archived && (old == null || old.done || old.archived) && all.count { it.id != record.id && it.kind == "TASK" && !it.done && !it.archived } >= 15) throw RuleViolation("premium_required")
        }
        if (old == null && !premium) {
            if (record.kind in listOf("WARRANTY", "CONTRACTOR", "EXPENSE", "APPOINTMENT")) throw RuleViolation("premium_required")
            if (record.repeat !in listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY")) throw RuleViolation("premium_required")
        }
        val saved = record.copy(updatedAt = System.currentTimeMillis())
        dao.put(saved)
        dao.event(HistoryEvent(recordId = saved.id, action = if (old == null) "created" else "updated", snapshot = gson.toJson(old ?: saved)))
    }

    fun validate(r: HomeRecord, all: List<HomeRecord>) {
        fun fail() : Nothing = throw RuleViolation("invalid_record")
        if (r.title.isBlank() || r.title.length > 200 || r.notes.length > 10000) fail()
        Kind.valueOf(r.kind)
        Repeat.valueOf(r.repeat)
        if (r.priority !in setOf("low", "normal", "high", "urgent")) fail()
        if (r.category !in setOf("general", "heating", "plumbing", "electrical", "roofing", "exterior", "garden", "cleaning", "ventilation", "safety", "appliances")) fail()
        val fields = runCatching { gson.fromJson(r.fields, com.google.gson.JsonObject::class.java) }.getOrElse { fail() }
        if (fields.size() > 50 || fields.entrySet().any { (key, value) -> key.length > 100 || !value.isJsonPrimitive || !value.asJsonPrimitive.isString || value.asString.length > 10000 }) fail()
        if (r.interval !in 1..3650 || r.anchorDay !in 1..31 || r.amountMinor < 0) fail()
        java.util.Currency.getInstance(r.currency)
        LocalDate.parse(r.date)
        r.due?.let { LocalDate.parse(it) }
        if (r.kind != "PROPERTY" && r.propertyId == null) fail()
        if (r.kind == "PROPERTY" && (r.propertyId != null || r.roomId != null || r.contractorId != null || r.relatedId != null)) fail()
        if (r.kind == "ROOM" && r.roomId != null) fail()
        fun ref(id: String?, kinds: Set<String>) {
            if (id == null) return
            val other = all.find { it.id == id } ?: fail()
            if (other.id == r.id || other.kind !in kinds) fail()
            if (other.kind != "PROPERTY" && other.propertyId != r.propertyId) fail()
        }
        ref(r.propertyId, setOf("PROPERTY")); ref(r.roomId, setOf("ROOM"))
        ref(r.contractorId, setOf("CONTRACTOR")); ref(r.relatedId, setOf("REPAIR", "APPLIANCE", "TASK"))
        if (r.kind == "REPAIR") RepairStatus.valueOf(r.status)
        if (r.repeat != "NONE" && (r.kind != "TASK" || r.due == null)) fail()
    }

    /** Completion advances exactly one scheduled occurrence, based on its due date, not wall clock. */
    suspend fun complete(id: String) = database.withTransaction {
        val r = dao.find(id) ?: return@withTransaction
        if (r.kind != "TASK" || r.done) return@withTransaction
        dao.event(HistoryEvent(recordId = id, action = "completed", snapshot = gson.toJson(r)))
        val next = if (!r.paused && r.due != null) Recurrence.next(LocalDate.parse(r.due), Repeat.valueOf(r.repeat), r.interval, r.anchorDay) else null
        dao.put(r.copy(done = next == null, due = next?.toString() ?: r.due, updatedAt = System.currentTimeMillis()))
    }

    suspend fun undoCompletion(id: String) = database.withTransaction {
        val last = dao.events().firstOrNull { it.recordId == id } ?: return@withTransaction
        if (last.action != "completed") throw RuleViolation("undo_unavailable")
        val prior = gson.fromJson(last.snapshot, HomeRecord::class.java)
        dao.put(prior.copy(updatedAt = System.currentTimeMillis()))
        dao.event(HistoryEvent(recordId = id, action = "undone", snapshot = last.snapshot))
    }

    suspend fun delete(id: String) = database.withTransaction { dao.delete(id) }
    suspend fun snooze(id: String) = database.withTransaction {
        val r = dao.find(id) ?: return@withTransaction
        // Snooze delivery independently of the recurrence anchor/due date.
        val map = gson.fromJson(r.fields, Map::class.java).toMutableMap()
        map["snoozedUntil"] = LocalDate.now().plusDays(1).toString()
        dao.put(r.copy(fields = gson.toJson(map), updatedAt = System.currentTimeMillis()))
        dao.event(HistoryEvent(recordId = id, action = "snoozed", snapshot = gson.toJson(r)))
    }
}
