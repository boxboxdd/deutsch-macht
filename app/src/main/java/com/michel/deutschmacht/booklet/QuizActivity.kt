package com.michel.deutschmacht.booklet

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.Store
import kotlin.random.Random

/** Short per-lesson quiz (8 questions, 4 options, both directions). */
class QuizActivity : AppCompatActivity() {

    private class Q(val prompt: String, val correct: String, options: List<String>) {
        val options: List<String> = options.shuffled()
    }

    private lateinit var questions: List<Q>
    private var qi = 0
    private var score = 0
    private lateinit var store: Store
    private var lesson = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)
        store = Store(this)
        lesson = intent.getIntExtra("lesson", 1)
        questions = build(lesson)
        show()
    }

    private fun build(lessonNum: Int): List<Q> {
        val l = Lessons.lesson(this, lessonNum)
        val all = Lessons.flat(this)
        val picks = l.entries.indices.shuffled()
        val n = minOf(8, picks.size)
        val out = ArrayList<Q>()
        for (k in 0 until n) {
            val e = l.entries[picks[k]]
            val deToFa = Random.nextBoolean()
            val prompt = if (deToFa) e.de else e.fa
            val correct = if (deToFa) e.fa else e.de
            val opts = ArrayList<String>()
            opts.add(correct)
            var guard = 0
            while (opts.size < 4 && guard++ < 200) {
                val other = all[Random.nextInt(all.size)]
                val cand = if (deToFa) other.fa else other.de
                if (cand !in opts) opts.add(cand)
            }
            out.add(Q(prompt, correct, opts))
        }
        return out
    }

    private fun show() {
        val title = findViewById<TextView>(R.id.txt_quiz_title)
        val progress = findViewById<TextView>(R.id.txt_quiz_progress)
        val prompt = findViewById<TextView>(R.id.txt_quiz_prompt)
        val opts = findViewById<LinearLayout>(R.id.quiz_options)
        findViewById<View>(R.id.txt_quiz_result).visibility = View.GONE
        findViewById<View>(R.id.btn_quiz_done).visibility = View.GONE

        if (qi >= questions.size) {
            finishQuiz()
            return
        }
        title.text = "آزمون — درس $lesson"
        progress.text = "${qi + 1} / ${questions.size}  •  امتیاز: $score"
        val q = questions[qi]
        prompt.text = q.prompt
        opts.removeAllViews()
        for (o in q.options) {
            val btn = MaterialButton(
                this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = o
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 10 }
                setOnClickListener {
                    val ok = o == q.correct
                    setBackgroundColor(
                        resources.getColor(
                            if (ok) android.R.color.holo_green_light
                            else android.R.color.holo_red_light,
                            theme
                        )
                    )
                    if (ok) score++
                    for (i in 0 until opts.childCount) opts.getChildAt(i).isEnabled = false
                    findViewById<View>(R.id.quiz_options).postDelayed({ qi++; show() }, 600)
                }
            }
            opts.addView(btn)
        }
    }

    private fun finishQuiz() {
        val pct = if (questions.isEmpty()) 0 else (score * 100f / questions.size).toInt()
        store.saveScore(lesson, pct)
        findViewById<TextView>(R.id.txt_quiz_progress).text = ""
        findViewById<TextView>(R.id.txt_quiz_prompt).text = ""
        val opts = findViewById<LinearLayout>(R.id.quiz_options)
        opts.removeAllViews()
        findViewById<View>(R.id.txt_quiz_result).visibility = View.VISIBLE
        findViewById<TextView>(R.id.txt_quiz_result).text =
            "نتیجه: $score از ${questions.size}  ($pct٪)"
        val done = findViewById<MaterialButton>(R.id.btn_quiz_done)
        done.visibility = View.VISIBLE
        done.setOnClickListener { finish() }
        Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show()
    }
}
