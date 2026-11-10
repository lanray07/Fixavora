package com.fixavora.app

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.fixavora.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate

class WorkspaceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as FixavoraApp
    private val phone get() = InstrumentationRegistry.getArguments().getString("deviceClass") == "phone"

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
        if (phone) compose.onNodeWithTag("compact-navigation").performScrollToNode(hasText(text))
        val nodes = compose.onAllNodesWithText(text, useUnmergedTree = false)
        // First matching text is the navigation item on tablet layouts.
        nodes.onFirst().performScrollTo().performClick()
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(image)
        val dir = File(app.getExternalFilesDir(null), "screenshots/en-GB/" + if (phone) "compact" else "tablet").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun actualScreensAndLocalTaskJourney() {
        compose.runOnUiThread { compose.activity.requestedOrientation = if (phone) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        seed()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Willow House").fetchSemanticsNodes().isNotEmpty() }
        capture("01-overview")
        clickNav("Maintenance"); compose.onNodeWithText("Clean the kitchen extractor filter").assertExists(); capture("02-maintenance")
        clickNav("Repairs"); compose.onNodeWithText("Kitchen tap — dripping").assertExists(); capture("03-repairs")
        clickNav("Appliances"); compose.onNodeWithText("Washing machine").performClick(); compose.onNodeWithText("WM-100").assertExists(); capture("04-appliances")
        compose.onNodeWithContentDescription("Close").performClick()
        clickNav("Assistant"); compose.onNodeWithText("What needs your attention?").performTextInput("Check the kitchen tap tomorrow")
        compose.onNodeWithText("Prepare task").performClick(); compose.onNodeWithText("Review your task draft").assertExists(); capture("05-voice-assistant")
        // A proposal must not write until the separate editor save action.
        runBlocking { assertEquals(3, app.repository.dao.all().count { it.kind == "TASK" }) }
        compose.onNodeWithText("Review details before saving").performClick(); compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(10000) { runBlocking { app.repository.dao.all().count { it.kind == "TASK" } == 4 } }
        clickNav("Calendar"); capture("06-calendar")
        clickNav("Maintenance"); compose.onNodeWithText("Check the kitchen tap tomorrow").assertExists(); capture("07-offline-records")
        clickNav("Reports"); compose.onNodeWithText("Report preview").assertExists(); capture("08-reports")
    }
}
