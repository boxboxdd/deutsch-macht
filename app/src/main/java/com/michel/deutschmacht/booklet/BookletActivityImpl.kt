package com.michel.deutschmacht.booklet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.michel.deutschmacht.R

/** Hosts the lesson-entry list opened from the Coach tab. */
class BookletActivityImpl : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plain)
        val lesson = intent.getIntExtra("lesson", 1)
        if (supportFragmentManager.findFragmentById(R.id.plain_container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.plain_container, LessonEntriesFragment.newInstance(lesson))
                .commit()
        }
    }
}
