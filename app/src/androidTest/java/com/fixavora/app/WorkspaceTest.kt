package com.fixavora.app

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.fixavora.app.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate

abstract class WorkspaceJourney<T : MainActivity>(activityClass: Class<T>, private val phone: Boolean) {
    @get:Rule val compose = createAndroidComposeRule(activityClass)
    private val app get() = compose.activity.application as FixavoraApp

    private fun seed() = runBlocking {
        app.repository.dao.clear()
        app.settings.text("language", "en-GB"); app.settings.flag("onboarding", true)
        val today = LocalDate.now()
        val home = HomeRecord(id = "demo-home", kind = "PROPERTY", title = "Willow House", notes = "Fictional demonstration property")
        val kitchen = HomeRecord(id = "demo-kitchen", kind = "ROOM", title = "Kitchen", propertyId = home.id)
        val contractor = HomeRecord(id = "demo-contractor", kind = "CONTRACTOR", title = "Alex • Willow Plumbing", propertyId = home.id, fields = "{\"trade\":\"Plumbing\",\"business_name\":\"Fictional demonstration business\"}")
        listOf(home, kitchen, contractor,
            HomeRecord(id = "demo-task-1", kind = "TASK", title = "Clean the kitchen extractor filter", propertyId = home.id, roomId = kitchen.id, due = today.plusDays(2).toString(), repeat = "MONTHLY", anchorDay = today.plusDays(2).dayOfMonth, category = "cleaning"),
            HomeRecord(id = "demo-task-2", kind = "TASK", title = "Arrange annual boiler service", propertyId = home.id, due = today.plusDays(5).toString(), repeat = "YEARLY", category = "heating", notes = "Book a qualified professional; follow manufacturer guidance.", anchorDay = today.plusDays(5).dayOfMonth),
            HomeRecord(id = "demo-task-3", kind = "TASK", title = "Review gutter cleaning appointment", propertyId = home.id, due = today.minusDays(2).toString(), category = "exterior"),
            HomeRecord(id = "demo-repair", kind = "REPAIR", title = "Kitchen tap — dripping", propertyId = home.id, roomId = kitchen.id, contractorId = contractor.id, status = "PLANNED", due = today.plusDays(3).toString(), fields = "{\"estimated_cost\":\"85.00\"}", category = "plumbing"),
            HomeRecord(id = "demo-appliance", kind = "APPLIANCE", title = "Washing machine", propertyId = home.id, roomId = kitchen.id, due = today.plusDays(25).toString(), fields = "{\"manufacturer\":\"Example Appliances\",\"model\":\"WM-100\",\"service_history\":\"Fictional demonstration record\"}"),
            HomeRecord(id = "demo-warranty", kind = "WARRANTY", title = "Washing machine cover", propertyId = home.id, relatedId = "demo-appliance", due = today.plusDays(25).toString()),
            HomeRecord(id = "demo-expense-1", kind = "EXPENSE", title = "Replacement tap washer", propertyId = home.id, roomId = kitchen.id, amountMinor = 1850, category = "plumbing"),
            HomeRecord(id = "demo-expense-2", kind = "EXPENSE", title = "Annual service visit", propertyId = home.id, contractorId = contractor.id, amountMinor = 9500, category = "heating"),
            HomeRecord(id = "demo-visit", kind = "APPOINTMENT", title = "Kitchen tap assessment", propertyId = home.id, contractorId = contractor.id, due = today.plusDays(3).toString(), fields = "{\"time\":\"10:00\"}")
        ).forEach { app.repository.save(it, true) }
    }

    private fun clickNav(text: String) {
        compose.waitUntil(10000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(10000) { compose.onAllNodesWithTag(if (phone) "compact-navigation" else "tablet-navigation").fetchSemanticsNodes().isNotEmpty() }
        if (phone) compose.onNodeWithTag("compact-navigation").performScrollToNode(hasText(text))
        else {
            repeat(4) {
                if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) compose.onNodeWithTag("tablet-navigation").performTouchInput { swipeDown() }
            }
            repeat(4) {
                if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) compose.onNodeWithTag("tablet-navigation").performTouchInput { swipeUp() }
            }
        }
        val nodes = compose.onAllNodesWithText(text, useUnmergedTree = false)
        // First matching text is the navigation item on tablet layouts.
        nodes.onFirst().performScrollTo().performClick()
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.waitUntil(8000) { compose.onAllNodesWithText("Saved on this device", substring = false).fetchSemanticsNodes().isEmpty() }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(image)
        assertTrue("Unexpected viewport orientation", if (phone) image.width < image.height else image.width > image.height)
        val dir = File(app.getExternalFilesDir(null), "screenshots/en-GB/" + if (phone) "compact" else "tablet").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun actualScreensAndLocalTaskJourney() {
        compose.runOnUiThread {
            compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        seed()
        try {
            compose.waitUntil(15000) { runCatching { compose.onAllNodesWithText("Willow House").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        } catch (error: Exception) {
            val diagnostics = File(app.getExternalFilesDir(null), "ui-startup-diagnostics.txt")
            diagnostics.writeText(runCatching { compose.onRoot().printToString() }.getOrElse { it.toString() } + "\n" +
                "activity=" + compose.activity.lifecycle.currentState + "\n" + runBlocking { "records=" + app.repository.dao.all().size + ", prefs=" + app.settings.flow.first() })
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { image ->
                File(app.getExternalFilesDir(null), "ui-startup-failure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
            }
            throw error
        }
        capture("01-overview")
        clickNav("Maintenance"); compose.onNodeWithText("Clean the kitchen extractor filter").assertExists(); capture("02-maintenance")
        clickNav("Repairs"); compose.onNodeWithText("Kitchen tap — dripping").assertExists(); capture("03-repairs")
        clickNav("Appliances"); compose.onNodeWithText("Washing machine").performClick(); compose.onNodeWithText("WM-100").assertExists(); capture("04-appliances")
        compose.onNodeWithContentDescription("Close").performClick()
        clickNav("Assistant"); compose.onNodeWithText("What needs your attention?").performTextInput("Check the kitchen tap tomorrow")
        compose.onNodeWithText("Prepare task").performClick(); compose.onNodeWithText("Review your task draft").assertExists(); capture("05-voice-assistant")
        // A proposal must not write until the separate editor save action.
        runBlocking { assertEquals(3, app.repository.dao.all().count { it.kind == "TASK" }) }
        compose.onNodeWithText("Review details before saving").performClick()
        try {
            compose.waitUntil(10000) { compose.onAllNodesWithText("Save", substring = false).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: Exception) {
            val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(app.getExternalFilesDir(null), "editor-failure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
            File(app.getExternalFilesDir(null), "editor-failure.txt").writeText(compose.onAllNodes(isRoot()).printToString())
            throw error
        }
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(10000) { runBlocking { app.repository.dao.all().count { it.kind == "TASK" } == 4 } }
        clickNav("Calendar"); capture("06-calendar")
        clickNav("Maintenance"); compose.onNodeWithText("Check the kitchen tap tomorrow").assertExists(); capture("07-offline-records")
        clickNav("Reports"); compose.onNodeWithText("Report preview").assertExists(); capture("08-reports")
    }
}

class WorkspaceTest : WorkspaceJourney<TabletCaptureActivity>(TabletCaptureActivity::class.java, false)
class CompactWorkspaceTest : WorkspaceJourney<CompactCaptureActivity>(CompactCaptureActivity::class.java, true)
