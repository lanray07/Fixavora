package com.fixavora.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Created and used on the UI thread; no audio is retained by Fixavora. Engines may use a network. */
class VoiceController(private val context: Context, private val changed: (Boolean, String?, String?) -> Unit) : RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false
    val available get() = SpeechRecognizer.isRecognitionAvailable(context)
    fun start(locale: Locale) {
        if (!available) { changed(false, null, "voice_unavailable"); return }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
            changed(true, null, null)
            recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag()).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1))
        } catch (_: Exception) { changed(false, null, "voice_unavailable") }
    }
    fun stop() { recognizer?.cancel(); changed(false, null, null) }
    fun speak(text: String, locale: Locale, unsupported: () -> Unit) {
        fun play() {
            val engine = tts ?: return
            if (!ready || engine.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) { unsupported(); return }
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fixavora-feedback") == TextToSpeech.ERROR) unsupported()
        }
        if (tts == null) tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS; play() } else play()
    }
    fun close() { recognizer?.destroy(); tts?.stop(); tts?.shutdown() }
    override fun onResults(results: Bundle?) { changed(false, results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(), null) }
    override fun onError(error: Int) { changed(false, null, if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "microphone_denied" else "voice_error") }
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
