package com.michel.deutschmacht.booklet

import android.content.Context
import android.content.Intent

/** Opens the booklet at a specific lesson. */
object BookletActivity {
    fun intent(context: Context, lesson: Int): Intent =
        Intent(context, BookletActivityImpl::class.java).putExtra("lesson", lesson)
}
