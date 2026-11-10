package com.fixavora.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.fixavora.app.data.*
import com.fixavora.app.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Destructive only to the explicitly disposable debug fixture installation. */
class SettingsDeletionTest {
    @get:Rule val compose = createAndroidComposeRule<EditorTestActivity>()
    @Test fun deletionRequiresConfirmationAndClearsRecordsAndPreferences() = runBlocking {
        val app=compose.activity.application as FixavoraApp
        app.repository.dao.clear()
        app.repository.save(HomeRecord(kind="PROPERTY",title="Disposable deletion fixture"),false)
        app.settings.text("language","en-GB"); app.settings.flag("spoken",true)
        val vm=ViewModelProvider(compose.activity)[HomeViewModel::class.java]
        compose.setContent { FixavoraTheme(false,false) { SettingsPanel(vm,Preferences(language="en-GB")) } }
        compose.onNodeWithText("Delete all home data").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(1,app.repository.dao.all().size)
        compose.onNodeWithText("Delete all home data").performScrollTo().performClick()
        compose.onNodeWithText("Delete",substring=false).performClick()
        compose.waitUntil(10000) { runBlocking { app.repository.dao.all().isEmpty() && !app.settings.flow.first().spoken } }
        assertEquals(Preferences(),app.settings.flow.first())
        assertTrue(app.repository.dao.events().isEmpty())
        assertTrue(compose.activity.contentResolver.persistedUriPermissions.isEmpty())
        assertTrue(app.getSharedPreferences("notification_history",0).all.isEmpty())
    }
}
