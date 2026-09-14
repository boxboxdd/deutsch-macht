package com.michel.deutschmacht.booklet;

import android.content.Context;
import android.content.Intent;

/** Helper to open the booklet at a specific lesson. */
public final class BookletActivity {
    private BookletActivity() {}

    public static Intent intent(Context c, int lesson) {
        Intent i = new Intent(c, BookletActivityImpl.class);
        i.putExtra("lesson", lesson);
        return i;
    }
}
