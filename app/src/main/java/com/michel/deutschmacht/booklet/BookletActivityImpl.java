package com.michel.deutschmacht.booklet;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.michel.deutschmacht.R;

/** Hosts the lesson-entry list opened from the Coach tab. */
public class BookletActivityImpl extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_plain);
        int lesson = getIntent().getIntExtra("lesson", 1);
        if (getSupportFragmentManager().findFragmentById(R.id.plain_container) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.plain_container, LessonEntriesFragment.newInstance(lesson))
                    .commit();
        }
    }
}
