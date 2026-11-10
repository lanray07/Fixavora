package com.fixavora.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.fixavora.app.data.*
import com.fixavora.app.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/** Real local DocumentsUI picker, encrypted backup and transactional restore.
 * Run through sandbox_qa.py after a real App Tester purchase has been verified. */
class DocumentBackupTest {
    @get:Rule val compose=createAndroidComposeRule<TabletCaptureActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun localDownloads() {
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.documentsui")),10000))
        val roots=device.findObject(By.pkg("com.android.documentsui").desc("Show roots"))
        roots?.click()
        val downloads=device.wait(Until.findObject(By.pkg("com.android.documentsui").res("android:id/title").text("Downloads")),5000)
        if(downloads==null) diagnostic("downloads-root")
        assertNotNull("An explicit local Downloads root is required",downloads);downloads.click()
    }
    private fun diagnostic(name:String) {
        val c=InstrumentationRegistry.getInstrumentation().targetContext
        device.dumpWindowHierarchy(File(c.getExternalFilesDir(null),"$name.xml"))
    }
    private fun createFile(name:String) {
        localDownloads()
        diagnostic("create-document-initial")
        device.takeScreenshot(File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"create-document-initial.png"))
        var field=device.wait(Until.findObject(By.pkg("com.android.documentsui").clazz("android.widget.EditText").textStartsWith("Fixavora-")),3000)
        if(field==null){
            // Fire's DocumentsUI first asks for the destination, then opens its
            // filename dialog through the visible Add action.
            device.findObject(By.res("com.android.documentsui","option_menu_add"))?.click()
            field=device.wait(Until.findObject(By.pkg("com.android.documentsui").clazz("android.widget.EditText").textStartsWith("Fixavora-")),5000)
        }
        if(field==null)diagnostic("create-document")
        assertNotNull("File name field",field);field.text=name
        val save=device.wait(Until.findObject(By.pkg("com.android.documentsui").text(Pattern.compile("(?i)^save$"))),5000)
        if(save==null)diagnostic("save-document")
        assertNotNull("Save document control",save);save.click()
        val returned=device.wait(Until.gone(By.pkg("com.android.documentsui")),15000)
        if(!returned)diagnostic("after-create")
        assertTrue("The system picker must return to Fixavora after saving",returned)
    }
    private fun chooseFile(name:String) {
        localDownloads()
        val row=device.wait(Until.findObject(By.pkg("com.android.documentsui").text(name)),7000)
        if(row==null)diagnostic("open-document")
        assertNotNull("Created QA document must appear in the real picker",row);row.click()
    }
    @Test fun encryptedBackupUsesPickerAndRestoresSavedRecords()=runBlocking {
        val app=compose.activity.application as FixavoraApp
        app.repository.dao.clear()
        val property=HomeRecord(kind="PROPERTY",title="Disposable backup QA home")
        app.repository.save(property,false)
        app.settings.flag("onboarding",true);app.settings.text("language","en-GB")
        val vm=ViewModelProvider(compose.activity)[HomeViewModel::class.java]
        compose.waitUntil(15000){compose.onAllNodesWithText(property.title).fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodesWithText("Settings").onFirst().performScrollTo().performClick()
        compose.waitUntil(60000){vm.billing.state.value.premium&&!vm.billing.state.value.busy}
        val name="Fixavora-QA-Backup-${System.nanoTime()}.fixavora"
        val password="Disposable QA backup passphrase"
        val pickerHost=compose.activity
        compose.onNodeWithText("Create encrypted backup").performScrollTo().performClick()
        compose.onNodeWithText("Backup passphrase").performTextInput(password)
        compose.onNodeWithText("Continue",substring=false).performClick()
        // Recreate the app while the external picker is open. The passphrase must
        // survive in memory without being placed in Android saved-instance state.
        InstrumentationRegistry.getInstrumentation().runOnMainSync { pickerHost.recreate() }
        createFile(name)
        compose.waitUntil(30000){vm.message.value=="backup_saved" || runCatching { compose.onAllNodesWithText(compose.activity.localized("en-GB").t("backup_saved")).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)}
        vm.message.value=null
        app.repository.save(property.copy(title="Changed after creating the backup"),true)
        compose.waitUntil(15000) { runCatching { compose.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        // Activity recreation rebuilds navigation; return to Settings after the
        // asynchronous document-result callback has finished writing the backup.
        compose.onAllNodesWithText("Settings").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Restore a backup").performScrollTo().performClick()
        compose.onNodeWithText("Backup passphrase").performTextInput(password)
        compose.onNodeWithText("Continue",substring=false).performClick()
        chooseFile(name)
        compose.onNode(hasText("Restore a backup") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(30000){runBlocking { app.repository.dao.find(property.id)?.title==property.title }}
        assertEquals(property.title,app.repository.dao.find(property.id)!!.title)
        compose.onAllNodesWithText("Reports").onFirst().performScrollTo().performClick()
        for(format in listOf("PDF","CSV")) {
            val exportName="Fixavora-QA-Export-${System.nanoTime()}.${format.lowercase()}"
            compose.onNodeWithText(format,substring=false).performClick()
            compose.onNodeWithText("Continue",substring=false).performClick()
            val exportHost=compose.activity
            InstrumentationRegistry.getInstrumentation().runOnMainSync { exportHost.recreate() }
            createFile(exportName)
            compose.waitUntil(30000) { vm.message.value=="export_saved" || runCatching { compose.onAllNodesWithText(compose.activity.localized("en-GB").t("export_saved")).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
            val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat /sdcard/Download/$exportName")
            val bytes=android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            if(format=="PDF") {
                assertTrue(String(bytes.take(5).toByteArray(),Charsets.US_ASCII).startsWith("%PDF-"))
                val check=File(app.cacheDir,"qa-export.pdf");check.writeBytes(bytes)
                android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(check,android.os.ParcelFileDescriptor.MODE_READ_ONLY)).use { assertTrue(it.pageCount>0) }
                check.delete()
            } else assertTrue("CSV must retain the reviewed records across recreation",bytes.toString(Charsets.UTF_8).contains(property.title))
            vm.message.value=null
            compose.waitUntil(15000) { runCatching { compose.onAllNodesWithText("Reports").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        }
        File(app.getExternalFilesDir(null),"document-backup-result.json").writeText("""{"create_document":"PASS","encrypted_write":"PASS","open_document":"PASS","restore_confirmation":"PASS","restored_records":"PASS","fixture_file":"$name"}""")
    }
}
