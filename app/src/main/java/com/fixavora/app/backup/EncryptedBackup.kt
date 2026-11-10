package com.fixavora.app.backup

import com.fixavora.app.data.*
import com.google.gson.Gson
import java.io.*
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.*
import androidx.room.withTransaction

data class BackupData(val version: Int = 1, val records: List<HomeRecord>, val events: List<HistoryEvent>)

/** Portable encrypted record backup. SAF attachments remain external and must be relinked after restore. */
object EncryptedBackup {
    private val magic = "FIXAVORA1".toByteArray()
    private const val MAX_BYTES = 20 * 1024 * 1024
    private fun key(password: CharArray, salt: ByteArray): javax.crypto.SecretKey {
        require(password.size >= 12)
        val spec = PBEKeySpec(password, salt, 210000, 256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES") } finally { spec.clearPassword() }
    }
    fun encode(data: BackupData, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv)); cipher.updateAAD(magic)
        val clear = Gson().toJson(data).toByteArray(Charsets.UTF_8)
        require(clear.size <= MAX_BYTES)
        return magic + salt + iv + cipher.doFinal(clear)
    }
    fun decode(bytes: ByteArray, password: CharArray): BackupData {
        require(bytes.size in (magic.size + 44)..MAX_BYTES + 100)
        require(bytes.copyOfRange(0, magic.size).contentEquals(magic))
        val start = magic.size
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, bytes.copyOfRange(start, start + 16)), GCMParameterSpec(128, bytes.copyOfRange(start + 16, start + 28))); cipher.updateAAD(magic)
        val data = Gson().fromJson(String(cipher.doFinal(bytes.copyOfRange(start + 28, bytes.size)), Charsets.UTF_8), BackupData::class.java)
        require(data.version == 1 && data.records.size <= 50000 && data.records.map { it.id }.distinct().size == data.records.size)
        return data
    }
    suspend fun restore(repo: HomeRepository, data: BackupData) = repo.database.withTransaction {
        // Validate everything before touching current data. Rebuild topologically for FK constraints.
        data.records.forEach { repo.validate(it, data.records) }
        require(data.events.all { e -> data.records.any { it.id == e.recordId } })
        repo.dao.clear()
        val pending = data.records.toMutableList()
        val inserted = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val ready = pending.filter { r -> listOfNotNull(r.propertyId, r.roomId, r.contractorId, r.relatedId).all { it in inserted } }
            require(ready.isNotEmpty())
            ready.forEach { repo.dao.put(it); inserted.add(it.id) }; pending.removeAll(ready.toSet())
        }
        data.events.forEach { repo.dao.event(it) }
    }
}
