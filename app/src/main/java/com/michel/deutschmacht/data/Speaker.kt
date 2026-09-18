package com.michel.deutschmacht.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Minimal German TextToSpeech wrapper. Uses the on-device (offline) engine when available. */
object Speaker {

    private val german = Locale("de", "DE")

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    @Synchronized
    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
        tts?.setLanguage(german)
    }

    @Synchronized
    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready) return
        engine.setLanguage(german)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "de")
    }
}
