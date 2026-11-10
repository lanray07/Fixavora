package com.fixavora.app.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.fixavora.app.*
import com.fixavora.app.R
import com.fixavora.app.ui.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as FixavoraApp
        val prefs = app.settings.flow.first()
        if (!prefs.reminders) return Result.success()
        val hour = LocalTime.now().hour
        val quiet = if (prefs.quietStart < prefs.quietEnd) hour in prefs.quietStart until prefs.quietEnd else hour >= prefs.quietStart || hour < prefs.quietEnd
        if (quiet) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("home-maintenance", app.localized(prefs.language).t("reminders"), NotificationManager.IMPORTANCE_DEFAULT))
        val log = app.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val all = app.repository.dao.all()
        val archivedProperties = all.filter { it.kind == "PROPERTY" && it.archived }.map { it.id }.toSet()
        all.filter { !it.done && !it.archived && it.propertyId !in archivedProperties && it.kind in setOf("TASK", "REPAIR", "APPOINTMENT", "WARRANTY", "APPLIANCE") && it.status !in setOf("COMPLETED", "CANCELLED") }.forEach { r ->
            val due = r.due?.let(LocalDate::parse) ?: return@forEach
            val threshold = if (r.kind in setOf("WARRANTY", "APPLIANCE")) today.plusDays(30) else today
            val snoozed = runCatching { Gson().fromJson(r.fields, Map::class.java)["snoozedUntil"]?.toString()?.let(LocalDate::parse) }.getOrNull()
            if (due > threshold || (snoozed != null && snoozed > today) || log.getString(r.id, "") == today.toString()) return@forEach
            val intent = Intent(app, MainActivity::class.java)
            val pending = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            // Hide private titles on the lock screen.
            manager.notify(r.id.hashCode(), NotificationCompat.Builder(app, "home-maintenance").setSmallIcon(R.drawable.ic_monochrome)
                .setContentTitle(app.localized(prefs.language).t("reminder_due")).setContentText(r.title).setContentIntent(pending)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).build())
            log.edit().putString(r.id, today.toString()).apply()
        }
        return Result.success()
    }
}
