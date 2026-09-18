package com.michel.deutschmacht.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * All persistent state: learned/hard flags, quiz scores, daily activity, streak, spaced repetition.
 * Backed by SharedPreferences — fully offline, no server.
 */
class Store(context: Context) {

    companion object {
        private const val NAME = "deutsch_macht"
        const val KEY_HIDE_DE = "hide_german"
        const val KEY_COMMIT = "commitment"
        const val KEY_PLACEMENT_DONE = "placement_done"
        private const val CHECK_DONE = "check_done_"
        private val DAY_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun day(): String {
            val c = Calendar.getInstance()
            return String.format("%04d-%02d-%02d", c[Calendar.YEAR], c[Calendar.MONTH] + 1, c[Calendar.DAY_OF_MONTH])
        }
    }

    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** Raw access for the few plain key/values (last lesson, placement result...). */
    fun prefs() = prefs

    private fun today(): String = day()

    // ---- per-entry flags ----

    fun isLearned(lesson: Int, index: Int): Boolean = prefs.getBoolean("l${lesson}_$index", false)

    fun setLearned(lesson: Int, index: Int, v: Boolean) {
        prefs.edit().putBoolean("l${lesson}_$index", v).apply()
        if (v) touchToday()
    }

    fun isHard(lesson: Int, index: Int): Boolean = prefs.getBoolean("h${lesson}_$index", false)

    fun setHard(lesson: Int, index: Int, v: Boolean) {
        prefs.edit().putBoolean("h${lesson}_$index", v).apply()
    }

    fun countLearned(lesson: Int, obj: Lessons.Lesson?): Int {
        val entries = obj?.entries ?: return 0
        var c = 0
        for (i in entries.indices) if (isLearned(lesson, i)) c++
        return c
    }

    fun countLearnedAll(context: Context): Int =
        Lessons.load(context).sumOf { countLearned(it.num, it) }

    // ---- quiz scores ----

    fun bestScore(lesson: Int): Int = prefs.getInt("q$lesson", -1)

    fun saveScore(lesson: Int, score: Int) {
        if (score > bestScore(lesson)) prefs.edit().putInt("q$lesson", score).apply()
        touchToday()
    }

    // ---- daily activity (30-day calendar + streak) ----

    /** Records activity for today. */
    fun touchToday() {
        val days = HashSet(prefs.getStringSet("days", emptySet())!!)
        days.add(today())
        prefs.edit().putStringSet("days", days).apply()
    }

    fun activeDays(): Array<String> = prefs.getStringSet("days", emptySet())!!.toTypedArray()

    fun isActiveToday(): Boolean = activeDays().contains(today())

    fun streak(): Int {
        val days = prefs.getStringSet("days", emptySet())!!
        var streak = 0
        val c = Calendar.getInstance()
        // If today has no activity yet, count from yesterday — the streak survives until midnight.
        if (!days.contains(today())) c.add(Calendar.DAY_OF_MONTH, -1)
        for (i in 0 until 3650) {
            val d = DAY_FMT.format(c.time)
            if (days.contains(d)) {
                streak++
                c.add(Calendar.DAY_OF_MONTH, -1)
            } else break
        }
        return streak
    }

    // ---- daily checklist (5 items) ----

    fun checklistDone(): BooleanArray {
        val out = BooleanArray(5)
        val s = prefs.getStringSet(CHECK_DONE + today(), emptySet())!!
        for (v in s) runCatching { out[v.toInt()] = true }
        return out
    }

    fun setChecklist(i: Int, v: Boolean) {
        val key = CHECK_DONE + today()
        val s = HashSet(prefs.getStringSet(key, emptySet()))
        if (v) s.add(i.toString()) else s.remove(i.toString())
        prefs.edit().putStringSet(key, s).apply()
        if (v) touchToday()
    }

    // ---- commitment card ("cheating forbidden") ----

    fun commitment(): String? = prefs.getString(KEY_COMMIT, null)

    fun setCommitment(text: String) {
        prefs.edit().putString(KEY_COMMIT, text).apply()
    }

    // ---- placement ----

    fun placementDone(): Boolean = prefs.getBoolean(KEY_PLACEMENT_DONE, false)

    fun setPlacementDone(v: Boolean) {
        prefs.edit().putBoolean(KEY_PLACEMENT_DONE, v).apply()
    }

    // ---- spaced repetition (box per entry) ----

    fun srBox(lesson: Int, index: Int): Int = prefs.getInt("sr${lesson}_$index", 0)

    fun srCorrect(lesson: Int, index: Int) {
        prefs.edit().putInt("sr${lesson}_$index", minOf(srBox(lesson, index) + 1, 5)).apply()
    }

    fun srWrong(lesson: Int, index: Int) {
        prefs.edit().putInt("sr${lesson}_$index", 0).apply()
    }

    fun srDue(lesson: Int, index: Int): Long = prefs.getLong("srd${lesson}_$index", 0L)

    fun srSchedule(lesson: Int, index: Int) {
        val gaps = longArrayOf(
            0L,
            1000L * 60 * 10,
            1000L * 60 * 60,
            1000L * 60 * 60 * 8,
            1000L * 60 * 60 * 24,
            1000L * 60 * 60 * 24 * 3
        )
        val due = System.currentTimeMillis() + gaps[minOf(srBox(lesson, index), gaps.size - 1)]
        prefs.edit().putLong("srd${lesson}_$index", due).apply()
    }

    /** Entries currently due for review, optionally limited to one lesson. */
    fun srDueEntries(context: Context, onlyLesson: Int? = null, limit: Int): List<Lessons.Entry> {
        val out = mutableListOf<Lessons.Entry>()
        val now = System.currentTimeMillis()
        for (l in Lessons.load(context)) {
            if (onlyLesson != null && l.num != onlyLesson) continue
            for (e in l.entries) {
                if (srBox(e.lesson, e.index) > 0 && srDue(e.lesson, e.index) <= now) out.add(e)
            }
        }
        return if (out.size > limit) out.takeLast(limit) else out
    }

    fun dueCount(context: Context, onlyLesson: Int? = null): Int =
        srDueEntries(context, onlyLesson, Int.MAX_VALUE).size

    // ---- stats for the Progress tab ----

    fun quizTotal(): Int = prefs.all.keys.count { it.startsWith("q") }

    fun hardCount(): Int = prefs.all.entries.count {
        it.key.startsWith("h") && (it.value as? Boolean) == true
    }
}
