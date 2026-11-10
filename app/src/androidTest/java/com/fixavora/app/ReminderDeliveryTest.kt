package com.fixavora.app

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.fixavora.app.data.HomeRecord
import com.fixavora.app.notifications.ReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

/** Exercises the actual worker and OS notification manager with disposable debug data. */
class ReminderDeliveryTest {
    @Test fun dueRecordsDeliverOnceAndQuietHoursSuppressDelivery()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<FixavoraApp>()
        val manager=app.getSystemService(NotificationManager::class.java)
        app.repository.dao.clear();manager.cancelAll()
        val history=app.getSharedPreferences("notification_history",0)
        history.edit().clear().commit()
        app.settings.flag("reminders",true);app.settings.text("language","en-GB")
        app.settings.number("quietStart",0);app.settings.number("quietEnd",0)
        val home=HomeRecord(kind="PROPERTY",title="Disposable reminder QA home")
        app.repository.save(home,false)
        val task=HomeRecord(kind="TASK",title="Disposable due QA task",propertyId=home.id,due=LocalDate.now().toString())
        app.repository.save(task,false)
        suspend fun runWorker() { TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork() }
        try {
            runWorker()
            assertTrue(manager.activeNotifications.any { it.id==task.id.hashCode() })
            assertEquals(LocalDate.now().toString(),history.getString(task.id,null))
            manager.cancelAll();runWorker()
            assertTrue("Same-day duplicates must be suppressed",manager.activeNotifications.isEmpty())
            history.edit().clear().commit()
            val hour=LocalTime.now().hour
            app.settings.number("quietStart",hour);app.settings.number("quietEnd",(hour+2)%24)
            runWorker();assertTrue(manager.activeNotifications.isEmpty());assertFalse(history.contains(task.id))
            app.settings.flag("reminders",false);app.settings.number("quietStart",0);app.settings.number("quietEnd",0)
            runWorker();assertTrue(manager.activeNotifications.isEmpty())
        } finally {manager.cancelAll();history.edit().clear().commit();app.settings.flag("reminders",false)}
    }
}
