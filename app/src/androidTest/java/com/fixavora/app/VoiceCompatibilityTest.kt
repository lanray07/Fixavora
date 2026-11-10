package com.fixavora.app

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.fixavora.app.ui.languages
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VoiceCompatibilityTest {
    @Test fun recordEngineCapabilitiesAndSynthesizeFeedback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val init = CountDownLatch(1)
        var engine: TextToSpeech? = null
        var initialized = false
        instrumentation.runOnMainSync { engine = TextToSpeech(context) { status -> initialized = status == TextToSpeech.SUCCESS; init.countDown() } }
        init.await(15, TimeUnit.SECONDS)
        val rows = languages.keys.filter { it.isNotEmpty() }.map { tag ->
            mapOf("locale" to tag, "ttsLanguageAvailable" to (initialized && engine!!.isLanguageAvailable(Locale.forLanguageTag(tag)) >= TextToSpeech.LANG_AVAILABLE), "acousticRecognitionTest" to "NOT TESTED")
        }
        var synthesisStatus = "NOT TESTED"
        val done = CountDownLatch(1)
        val output = File(context.cacheDir, "tts-verification.wav")
        if (initialized && engine!!.setLanguage(Locale.UK) >= TextToSpeech.LANG_AVAILABLE) {
            engine!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { synthesisStatus = if (output.length() > 44) "PASS" else "FAIL"; done.countDown() }
                override fun onError(utteranceId: String?) { synthesisStatus = "FAIL"; done.countDown() }
            })
            val result = engine!!.synthesizeToFile("Your maintenance record is saved.", null, output, "verification")
            if (result == TextToSpeech.SUCCESS) done.await(20, TimeUnit.SECONDS) else synthesisStatus = "FAIL"
        }
        val report = mapOf("model" to android.os.Build.MODEL, "api" to android.os.Build.VERSION.SDK_INT, "recognitionServiceAvailable" to SpeechRecognizer.isRecognitionAvailable(context), "ttsInitialized" to initialized, "en-GB_synthesis" to synthesisStatus, "locales" to rows)
        File(context.getExternalFilesDir(null), "voice-compatibility.json").writeText(Gson().toJson(report))
        engine?.shutdown(); output.delete()
        assertEquals(19, rows.size)
        // Missing engines/languages are valid capabilities, not a fabricated recognition success.
    }
}
