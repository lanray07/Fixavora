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
import com.fixavora.app.ui.localized
import com.fixavora.app.ui.t
import com.google.gson.Gson

abstract class WorkspaceJourney<T : MainActivity>(activityClass: Class<T>, private val phone: Boolean) {
    @get:Rule val compose = createAndroidComposeRule(activityClass)
    private val app get() = compose.activity.application as FixavoraApp
    private val locale get() = InstrumentationRegistry.getArguments().getString("locale") ?: "en-GB"
    private val style get() = InstrumentationRegistry.getArguments().getString("style") ?: ""
    private val variant get() = (if(phone) "compact" else "tablet") + if(style.isEmpty()) "" else "-$style"
    private val copy by lazy {
        val stream = InstrumentationRegistry.getInstrumentation().context.assets.open("screenshots/$locale.json")
        stream.bufferedReader().use { Gson().fromJson(it, com.google.gson.JsonObject::class.java) }
    }
    private fun fixture(key: String) = copy[key].asString
    private fun label(key: String) = compose.activity.localized(locale).t(key)
    private fun fields(vararg entries: Pair<String,String>) = Gson().toJson(mapOf(*entries))

    private fun seed() = runBlocking {
        app.repository.dao.clear()
        app.settings.text("language", locale); app.settings.flag("onboarding", true)
        app.settings.flag("dark",style.contains("dark"));app.settings.flag("contrast",style.contains("contrast"))
        val today = LocalDate.now()
        val home = HomeRecord(id = "demo-home", kind = "PROPERTY", title = fixture("home"), notes = fixture("fictional_property"))
        val kitchen = HomeRecord(id = "demo-kitchen", kind = "ROOM", title = fixture("kitchen"), propertyId = home.id)
        val contractor = HomeRecord(id = "demo-contractor", kind = "CONTRACTOR", title = fixture("contractor"), propertyId = home.id, fields = fields("trade" to fixture("trade"), "business_name" to fixture("fictional_business")))
        listOf(home, kitchen, contractor,
            HomeRecord(id = "demo-task-1", kind = "TASK", title = fixture("task_1"), propertyId = home.id, roomId = kitchen.id, due = today.plusDays(2).toString(), repeat = "MONTHLY", anchorDay = today.plusDays(2).dayOfMonth, category = "cleaning"),
            HomeRecord(id = "demo-task-2", kind = "TASK", title = fixture("task_2"), propertyId = home.id, due = today.plusDays(5).toString(), repeat = "YEARLY", category = "heating", notes = fixture("professional_note"), anchorDay = today.plusDays(5).dayOfMonth),
            HomeRecord(id = "demo-task-3", kind = "TASK", title = fixture("task_3"), propertyId = home.id, due = today.minusDays(2).toString(), category = "exterior"),
            HomeRecord(id = "demo-repair", kind = "REPAIR", title = fixture("repair"), propertyId = home.id, roomId = kitchen.id, contractorId = contractor.id, status = "PLANNED", due = today.plusDays(3).toString(), fields = "{\"estimated_cost\":\"85.00\"}", category = "plumbing"),
            HomeRecord(id = "demo-appliance", kind = "APPLIANCE", title = fixture("appliance"), propertyId = home.id, roomId = kitchen.id, due = today.plusDays(25).toString(), fields = fields("manufacturer" to fixture("manufacturer"), "model" to "WM-100", "service_history" to fixture("fictional_record"))),
            HomeRecord(id = "demo-warranty", kind = "WARRANTY", title = fixture("warranty"), propertyId = home.id, relatedId = "demo-appliance", due = today.plusDays(25).toString()),
            HomeRecord(id = "demo-expense-1", kind = "EXPENSE", title = fixture("expense_1"), propertyId = home.id, roomId = kitchen.id, amountMinor = 1850, category = "plumbing"),
            HomeRecord(id = "demo-expense-2", kind = "EXPENSE", title = fixture("expense_2"), propertyId = home.id, contractorId = contractor.id, amountMinor = 9500, category = "heating"),
            HomeRecord(id = "demo-visit", kind = "APPOINTMENT", title = fixture("appointment"), propertyId = home.id, contractorId = contractor.id, due = today.plusDays(3).toString(), fields = "{\"time\":\"10:00\"}")
        ).forEach { app.repository.save(it, true) }
    }

    private fun clickNav(key: String) {
        val text = label(key)
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
        compose.waitUntil(8000) { compose.onAllNodesWithText(label("saved"), substring = false).fetchSemanticsNodes().isEmpty() }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(image)
        assertTrue("Unexpected viewport orientation", if (phone) image.width < image.height else image.width > image.height)
        val dir = File(app.getExternalFilesDir(null), "screenshots/$locale/$variant").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val index=name.substringBefore('-').toInt()
        if(style.isEmpty()) StoreComposition.write(image,File(app.getExternalFilesDir(null),"screenshots/$locale/composed/$variant/$name.png"),fixture("headline_$index"),fixture("caption_$index"),locale,phone)
        image.recycle()
    }

    @Test fun actualScreensAndLocalTaskJourney() {
        compose.runOnUiThread {
            compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        seed()
        try {
            compose.waitUntil(15000) { runCatching { compose.onAllNodesWithText(fixture("home")).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
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
        clickNav("tasks"); compose.onNodeWithText(fixture("task_1")).assertExists(); capture("02-maintenance")
        clickNav("repairs"); compose.onNodeWithText(fixture("repair")).assertExists(); capture("03-repairs")
        clickNav("appliances"); compose.onNodeWithText(fixture("appliance")).performClick(); compose.onNodeWithText("WM-100").assertExists(); capture("04-appliances")
        compose.onNodeWithContentDescription(label("close")).performClick()
        clickNav("assistant")
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(compose.activity)) {
            compose.onNodeWithText(label("push_to_talk")).performClick()
            compose.onAllNodesWithText(label("voice_unavailable")).onFirst().assertExists()
            // One persistent capability notice remains after the Snackbar clears.
            compose.waitUntil(10000) { compose.onAllNodesWithText(label("voice_unavailable")).fetchSemanticsNodes().size == 1 }
        }
        compose.onNodeWithText(label("type_or_speak")).performTextInput(fixture("proposal"))
        compose.onNodeWithText(label("prepare_task")).performClick(); compose.onNodeWithText(label("review_proposal")).assertExists(); capture("05-voice-assistant")
        // A proposal must not write until the separate editor save action.
        runBlocking { assertEquals(3, app.repository.dao.all().count { it.kind == "TASK" }) }
        compose.onNodeWithText(label("review_and_save")).performClick()
        try {
            compose.waitUntil(10000) { compose.onAllNodesWithText(label("save"), substring = false).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: Exception) {
            val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(app.getExternalFilesDir(null), "editor-failure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
            File(app.getExternalFilesDir(null), "editor-failure.txt").writeText(compose.onAllNodes(isRoot()).printToString())
            throw error
        }
        compose.onNodeWithText(label("save"), substring = false).performClick()
        compose.waitUntil(10000) { runBlocking { app.repository.dao.all().count { it.kind == "TASK" } == 4 } }
        clickNav("calendar"); capture("06-calendar")
        clickNav("tasks"); compose.onNodeWithText(fixture("proposal")).assertExists(); capture("07-offline-records")
        clickNav("reports"); compose.onNodeWithText(label("preview")).assertExists(); capture("08-reports")
    }
}

class WorkspaceTest : WorkspaceJourney<TabletCaptureActivity>(TabletCaptureActivity::class.java, false)
class CompactWorkspaceTest : WorkspaceJourney<CompactCaptureActivity>(CompactCaptureActivity::class.java, true)
