package com.michel.deutschmacht.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** German text-to-speech singleton. */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.GERMANY
                pending?.let { speak(it); pending = null }
            }
        }
    }

    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "de")
        else pending = text
    }

    fun shutdown() {
        tts?.shutdown(); tts = null; ready = false
    }
}
