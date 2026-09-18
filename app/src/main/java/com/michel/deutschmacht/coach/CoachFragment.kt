package com.michel.deutschmacht.coach

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.michel.deutschmacht.R
import com.michel.deutschmacht.booklet.BookletActivity
import com.michel.deutschmacht.booklet.QuizActivity
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.Store

/** Tab 1: hero + progress ring + no-cheating commitment + daily checklist + lesson picker. */
class CoachFragment : Fragment() {

    private lateinit var store: Store
    private var pickedLesson = 1

    private val checkIds = intArrayOf(R.id.chk1, R.id.chk2, R.id.chk3, R.id.chk4, R.id.chk5)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_coach, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = Store(requireContext())

        val total = Lessons.totalEntries(requireContext())
        val learned = store.countLearnedAll(requireContext())
        val pct = if (total == 0) 0 else (learned * 100f / total).toInt()

        view.findViewById<CircularProgressIndicator>(R.id.ring_progress).setProgress(pct)
        view.findViewById<TextView>(R.id.txt_ring_pct).text = "$pct%"
        view.findViewById<TextView>(R.id.txt_streak).text =
            "🔥 ${store.streak()} ${getString(R.string.streak_days)}  •  $learned ${getString(R.string.learned_words)}"

        setupCommitment(view)
        setupChecklist(view)
        setupLessonPicker(view)
    }

    private fun setupCommitment(view: View) {
        val saved = view.findViewById<TextView>(R.id.txt_commit_saved)
        val til = view.findViewById<TextInputLayout>(R.id.til_commit)
        val edt = view.findViewById<TextInputEditText>(R.id.edt_commit)
        val btn = view.findViewById<MaterialButton>(R.id.btn_commit)

        val existing = store.commitment()
        if (existing != null) {
            saved.visibility = View.VISIBLE
            saved.text = "${getString(R.string.commitment_saved)}\n«$existing»"
            til.visibility = View.GONE
            btn.visibility = View.GONE
        }

        btn.setOnClickListener {
            val t = edt.text?.toString()?.trim()
            if (t.isNullOrEmpty()) {
                Toast.makeText(context, getString(R.string.commitment_hint), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            store.setCommitment(t)
            saved.visibility = View.VISIBLE
            saved.text = "${getString(R.string.commitment_saved)}\n«$t»"
            til.visibility = View.GONE
            btn.visibility = View.GONE
        }
    }

    private fun setupChecklist(view: View) {
        val done = store.checklistDone()
        for (i in checkIds.indices) {
            val cb = view.findViewById<CheckBox>(checkIds[i])
            cb.isChecked = done[i]
            cb.setOnCheckedChangeListener { _, isChecked -> store.setChecklist(i, isChecked) }
        }
    }

    private fun setupLessonPicker(view: View) {
        val slider = view.findViewById<Slider>(R.id.slider_lesson)
        val pick = view.findViewById<TextView>(R.id.txt_lesson_pick)
        val stat = view.findViewById<TextView>(R.id.txt_lesson_stat)

        pickedLesson = store.prefs().getInt("last_lesson", 1).coerceIn(1, 10)
        slider.value = pickedLesson.toFloat()
        refreshPick(pick, stat)

        slider.addOnChangeListener { _, value, fromUser ->
            pickedLesson = value.toInt()
            store.prefs().edit().putInt("last_lesson", pickedLesson).apply()
            refreshPick(pick, stat)
        }

        view.findViewById<MaterialButton>(R.id.btn_start_lesson).setOnClickListener {
            val l = Lessons.lesson(requireContext(), pickedLesson)
            if (l.entries.isEmpty()) return@setOnClickListener
            startActivity(BookletActivity.intent(requireContext(), pickedLesson))
        }

        view.findViewById<MaterialButton>(R.id.btn_quiz_lesson).setOnClickListener {
            startActivity(Intent(requireContext(), QuizActivity::class.java).putExtra("lesson", pickedLesson))
        }
    }

    private fun refreshPick(pick: TextView, stat: TextView) {
        val l = Lessons.lesson(requireContext(), pickedLesson)
        pick.text = "${getString(R.string.start_lesson)} — درس $pickedLesson (${l.entries.size} جمله)"
        stat.text = "${store.countLearned(pickedLesson, l)} / ${l.entries.size} ${getString(R.string.learned_words)}"
    }
}
