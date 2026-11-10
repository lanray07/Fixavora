package com.fixavora.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.fixavora.app.data.*
import com.fixavora.app.domain.Kind
import com.fixavora.app.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.LinkedBlockingQueue

/** Uses the real editor and repository; Premium is an explicit repository test input,
 * never a production entitlement override. The app's own database is not touched. */
class ModuleFormsTest {
    @get:Rule val compose = createAndroidComposeRule<EditorTestActivity>()
    @Test fun everyRecordEditorCreatesEditsAndDeletesPersistentRecords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, HomeDatabase::class.java).build()
        val repo = HomeRepository(db)
        val property = HomeRecord(kind="PROPERTY", title="Isolated QA property")
        repo.save(property, true)
        val editing = mutableStateOf<HomeRecord?>(null)
        val results = LinkedBlockingQueue<HomeRecord>()
        compose.setContent { FixavoraTheme(false, false) { Box(Modifier.testTag("editor-test-host")) {
            Text("Editor test host")
            editing.value?.let { current -> key(current.id) { RecordEditor(current, listOf(property), { editing.value=null }, { result -> results.offer(result); editing.value=null }) } }
        } } }
        try {
            for (kind in Kind.entries) {
                val initial = newRecord(kind, property.id, Preferences())
                compose.runOnIdle { editing.value=initial }
                compose.waitUntil(10000) { compose.onAllNodesWithText("Name or title *").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Name or title *").performTextInput("QA ${kind.name}")
                compose.onNodeWithText("Notes").performScrollTo().performTextInput("A saved maintenance note")
                compose.onNodeWithText("Save", substring=false).performClick()
                val created = results.poll(5, java.util.concurrent.TimeUnit.SECONDS)
                assertNotNull("Editor did not save ${kind.name}", created)
                repo.save(created!!, true)
                val reopened = repo.dao.find(created.id)!!
                assertEquals("QA ${kind.name}",reopened.title)
                assertEquals("A saved maintenance note", reopened.notes)
                compose.runOnIdle { editing.value=reopened }
                compose.onNodeWithText("Name or title *").performTextReplacement("Updated ${kind.name}")
                compose.onNodeWithText("Save",substring=false).performClick()
                val edited=results.poll(5,java.util.concurrent.TimeUnit.SECONDS)!!
                repo.save(edited,true)
                assertEquals("Updated ${kind.name}",repo.dao.find(created.id)!!.title)
                repo.delete(created.id)
                assertNull(repo.dao.find(created.id))
            }
        } finally { db.close() }
    }
}
