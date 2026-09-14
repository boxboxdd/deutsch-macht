package com.michel.deutschmacht.data;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Simple German TTS wrapper (offline engine when available). */
public final class Speaker {

    private static TextToSpeech tts;
    private static boolean ready;

    private Speaker() {}

    public static void init(Context c) {
        if (tts != null) return;
        tts = new TextToSpeech(c.getApplicationContext(), status -> ready = status == TextToSpeech.SUCCESS);
        tts.setLanguage(new Locale("de", "DE"));
    }

    public static void speak(String text) {
        if (tts == null || !ready) return;
        tts.setLanguage(new Locale("de", "DE"));
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "de");
    }
}
