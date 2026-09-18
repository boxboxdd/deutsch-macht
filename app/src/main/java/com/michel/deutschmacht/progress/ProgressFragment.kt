package com.michel.deutschmacht.progress

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.Store

/** Tab 5: stats + per-lesson bars + 30-day calendar + badges. */
class ProgressFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = Store(requireContext())
        val lessons = Lessons.load(requireContext())

        val total = Lessons.totalEntries(requireContext())
        val learned = store.countLearnedAll(requireContext())

        view.findViewById<TextView>(R.id.stat_learned).text = "$learned / $total"
        view.findViewById<TextView>(R.id.stat_quiz).text = store.quizTotal().toString()
        view.findViewById<TextView>(R.id.stat_hard).text = store.hardCount().toString()

        val box = view.findViewById<LinearLayout>(R.id.lesson_progress_box)
        for (l in lessons) {
            val n = l.entries.size
            val learnedInLesson = store.countLearned(l.num, l)
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 12, 0, 12)
            }
            val title = TextView(requireContext()).apply {
                text = "درس ${l.num} — $learnedInLesson / $n"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(requireContext().getColor(R.color.green_dark))
            }
            row.addView(title)

            val bar = LinearProgressIndicator(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 6 }
                max = 100
                setProgress(if (n == 0) 0 else (learnedInLesson * 100f / n).toInt())
                trackThickness = 8
                trackCornerRadius = 4
            }
            row.addView(bar)
            box.addView(row)
        }

        view.findViewById<CalendarView30>(R.id.calendar).refresh(store)
        view.findViewById<BadgesView>(R.id.badges).refresh(store, total)
    }
}
