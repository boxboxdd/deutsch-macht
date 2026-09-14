package com.michel.deutschmacht.data

import android.content.Context
import org.json.JSONArray

data class Entry(val de: String, val fa: String)
data class Lesson(val num: Int, val entries: List<Entry>)

object Lessons {
    private var cached: List<Lesson> = emptyList()

    fun load(context: Context): List<Lesson> {
        if (cached.isNotEmpty()) return cached
        val raw = context.assets.open("lessons.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val list = mutableListOf<Lesson>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val es = o.getJSONArray("entries")
            val entries = mutableListOf<Entry>()
            for (j in 0 until es.length()) {
                val e = es.getJSONObject(j)
                entries += Entry(e.getString("de"), e.getString("fa"))
            }
            list += Lesson(o.getInt("num"), entries)
        }
        cached = list
        return cached
    }

    fun lesson(context: Context, num: Int): Lesson =
        load(context).first { it.num == num }
}
