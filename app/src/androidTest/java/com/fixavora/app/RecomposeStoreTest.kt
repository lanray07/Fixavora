package com.fixavora.app

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/** Re-typesets reviewed copy over previously captured, already localized real UI. */
class RecomposeStoreTest {
    @Test fun composeReviewedCaptionsWithActualLocalizedCaptures() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val tags=(InstrumentationRegistry.getArguments().getString("locales")?:"ja-JP").split(',')
        for(tag in tags) {
            val copy=instrumentation.context.assets.open("screenshots/$tag.json").bufferedReader().use {Gson().fromJson(it,com.google.gson.JsonObject::class.java)}
            for(variant in listOf("tablet","compact")) {
                val root=File(instrumentation.targetContext.getExternalFilesDir(null),"screenshots/$tag")
                val captures=File(root,variant).listFiles()?.filter {it.extension=="png"}.orEmpty()
                if(variant=="tablet")assertEquals("Eight real localized captures are required",8,captures.size)
                for(file in captures) {
                    val source=BitmapFactory.decodeFile(file.absolutePath);assertNotNull(source)
                    val index=file.name.substringBefore('-').toInt()
                    StoreComposition.write(source,File(root,"composed/$variant/${file.name}"),copy["headline_$index"].asString,copy["caption_$index"].asString,tag,variant=="compact")
                    source.recycle()
                }
            }
        }
    }
}
