package com.michel.deutschmacht.data

import android.content.Context

/** Tracks quiz best-scores and learned entries in SharedPreferences. */
class ProgressStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    /** Key: "L<lesson>:<index>" -> learned flag. */
    fun isLearned(lesson: Int, index: Int) =
        prefs.getBoolean("L$lesson:$index", false)

    fun toggleLearned(lesson: Int, index: Int, value: Boolean) =
        prefs.edit().putBoolean("L$lesson:$index", value).apply()

    fun countLearned(lesson: com.michel.deutschmacht.data.Lesson): Int =
        lesson.entries.indices.count { isLearned(lesson.num, it) }

    fun bestScore(lesson: Int): Int = prefs.getInt("score$lesson", -1)

    fun setBestScore(lesson: Int, score: Int) =
        prefs.edit().putInt("score$lesson", score).apply()
}
