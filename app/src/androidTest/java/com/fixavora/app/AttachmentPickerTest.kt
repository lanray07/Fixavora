package com.fixavora.app

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.fixavora.app.data.HomeRecord
import com.fixavora.app.ui.openAttachment
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

class AttachmentPickerTest {
    @get:Rule val compose=createAndroidComposeRule<TabletCaptureActivity>()
    private fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).bufferedReader().use {it.readText()}
    @Test fun systemDocumentPickerPersistsLinkAndRevokedPermissionFallsBack()=runBlocking {
        val app=compose.activity.application as FixavoraApp
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val name="Fixavora-QA-Document-${System.nanoTime()}.txt"
        val source=File(app.getExternalFilesDir(null),"disposable-attachment.txt").apply {writeText("Disposable_Fixavora_QA_document")}
        shell("cp ${source.absolutePath} /sdcard/Download/$name")
        assertTrue("The disposable file was actually created",shell("cat /sdcard/Download/$name").contains("Disposable_Fixavora_QA_document"))
        shell("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/$name")
        try {
            app.repository.dao.clear();app.settings.flag("onboarding",true);app.settings.text("language","en-GB")
            val home=HomeRecord(kind="PROPERTY",title="Disposable attachment QA home")
            app.repository.save(home,false)
            compose.waitUntil(15000) { compose.onAllNodesWithText(home.title).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Properties").onFirst().performScrollTo().performClick()
            compose.onAllNodesWithText(home.title).onLast().performClick()
            compose.onNodeWithText("Attach photo or document").performScrollTo().performClick()
            assertTrue(device.wait(Until.hasObject(By.pkg("com.android.documentsui")),10000))
            val pickerHost=compose.activity
            InstrumentationRegistry.getInstrumentation().runOnMainSync {pickerHost.recreate()}
            device.findObject(By.pkg("com.android.documentsui").desc("Show roots"))?.click()
            device.wait(Until.findObject(By.pkg("com.android.documentsui").res("android:id/title").text("Downloads")),5000)!!.click()
            val row=device.wait(Until.findObject(By.pkg("com.android.documentsui").text(name)),8000)
            assertNotNull("The actual disposable document appears in the system picker",row);row.click()
            compose.waitUntil(15000) { runBlocking { app.repository.dao.attachments().size==1 } }
            val attachment=app.repository.dao.attachments().single()
            assertTrue(attachment.name.contains(name))
            val uri=android.net.Uri.parse(attachment.uri)
            assertTrue(app.contentResolver.persistedUriPermissions.any {it.uri==uri&&it.isReadPermission})
            app.contentResolver.openInputStream(uri)!!.bufferedReader().use { assertTrue(it.readText().contains("Disposable_Fixavora_QA_document")) }
            app.contentResolver.releasePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            var failed=false
            // Delete only this test's unique file: a stale document link must fail
            // inside Fixavora rather than launch a viewer with an unreadable URI.
            shell("rm /sdcard/Download/$name")
            openAttachment(app,attachment) {failed=true};assertTrue(failed)
            compose.onNodeWithContentDescription("Remove attachment link").performClick()
            compose.waitUntil(10000) {runBlocking {app.repository.dao.attachments().isEmpty()} }
        } finally {shell("rm -f /sdcard/Download/$name");source.delete()}
    }
}
