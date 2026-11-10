package com.fixavora.app

import android.app.Application
import androidx.room.Room
import androidx.work.*
import com.fixavora.app.data.*
import com.fixavora.app.notifications.ReminderWorker
import java.util.concurrent.TimeUnit

class FixavoraApp : Application() {
    val database by lazy { Room.databaseBuilder(this, HomeDatabase::class.java, "fixavora.db").addMigrations(HomeDatabase.MIGRATION_1_2).build() }
    val repository by lazy { HomeRepository(database) }
    val settings by lazy { Settings(this) }
    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("maintenance-reminders", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build())
    }
}
