package com.michel.deutschmacht.data

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Loads [lessons.json] from assets. Duplicated booklet lines are intentionally kept:
 * the repetition itself is the Michel Thomas method.
 */
object Lessons {

    data class Entry(val de: String, val fa: String, val lesson: Int, val index: Int)

    data class Lesson(val num: Int, val entries: MutableList<Entry> = mutableListOf())

    @Volatile
    private var cache: List<Lesson>? = null

    private val lock = Any()

    fun load(context: Context): List<Lesson> = synchronized(lock) {
        cache?.let { return it }
        val parsed = try {
            parse(context)
        } catch (e: Exception) {
            emptyList()
        }
        cache = parsed
        parsed
    }

    private fun parse(context: Context): List<Lesson> {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(context.assets.open("lessons.json"), StandardCharsets.UTF_8)).use { r ->
            var line = r.readLine()
            while (line != null) {
                sb.append(line)
                line = r.readLine()
            }
        }
        val out = mutableListOf<Lesson>()
        val root = JSONArray(sb.toString())
        for (i in 0 until root.length()) {
            val lo = root.getJSONObject(i)
            val lesson = Lesson(lo.getInt("num"))
            val arr = lo.getJSONArray("entries")
            for (j in 0 until arr.length()) {
                val eo = arr.getJSONObject(j)
                lesson.entries.add(Entry(eo.getString("de"), eo.getString("fa"), lesson.num, j))
            }
            out.add(lesson)
        }
        return out
    }

    fun lesson(context: Context, num: Int): Lesson =
        load(context).firstOrNull { it.num == num } ?: Lesson(num)

    fun totalEntries(context: Context): Int = load(context).sumOf { it.entries.size }

    fun flat(context: Context): List<Entry> = load(context).flatMap { it.entries }
}
