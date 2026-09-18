package com.michel.deutschmacht.placement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.michel.deutschmacht.R
import com.michel.deutschmacht.booklet.BookletActivity
import com.michel.deutschmacht.data.Store
import kotlin.random.Random

/** Tab 2: 4-question diagnostic quiz that recommends a starting lesson + study guide. */
class PlacementFragment : Fragment() {

    private class Q(
        val prompt: String,
        val opts: Array<String>,
        val correct: Int,
        val lessonIfCorrect: Int
    )

    private val questions = arrayOf(
        Q("«ich will» یعنی چه؟",
            arrayOf("من می‌خواهم", "تو می‌خواهی", "ما می‌خواهیم", "آن‌ها می‌خواهند"), 0, 1),
        Q("«Wo ist es?» یعنی چه؟",
            arrayOf("این چیست؟", "کجاست؟", "چه زمانی؟", "چرا؟"), 1, 2),
        Q("جای خالی: «ich ___ nach Deutschland fahren» (می‌خواهم به آلمان بروم)",
            arrayOf("will", "möchte", "kann", "muss"), 1, 5),
        Q("«Ich kann es nicht finden» یعنی چه؟",
            arrayOf("من می‌توانم آن را ببینم", "من نمی‌توانم آن را پیدا کنم", "من آن را نمی‌خواهم", "من آن را نمی‌دانم"), 1, 4),
        Q("«Sie müssen hier bleiben» یعنی چه؟",
            arrayOf("شما باید اینجا بمانید", "شما می‌خواهید اینجا بمانید", "شما اینجا هستید", "شما اینجا کار می‌کنید"), 0, 6),
        Q("«Ich habe es gekauft» یعنی چه؟",
            arrayOf("من آن را می‌خرم", "من آن را خریدم", "من آن را می‌خواهم", "من آن را خواهم خرید"), 1, 8)
    )

    private var active: List<Q> = emptyList()
    private var qIndex = 0
    private var correctCount = 0
    private var suggested = 1
    private lateinit var store: Store

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_placement, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = Store(requireContext())

        view.findViewById<MaterialButton>(R.id.btn_start_quiz).setOnClickListener { startQuiz(view) }

        if (store.placementDone()) {
            view.findViewById<MaterialButton>(R.id.btn_start_quiz).setText(R.string.practice_again)
            showResult(view, store.prefs().getInt("placement_lesson", 1), true)
        } else {
            view.findViewById<TextView>(R.id.txt_question).text = "۴ سؤال — آماده‌ای؟"
        }
    }

    private fun startQuiz(view: View) {
        active = pick4()
        qIndex = 0
        correctCount = 0
        view.findViewById<MaterialButton>(R.id.btn_start_quiz).visibility = View.GONE
        view.findViewById<View>(R.id.card_result).visibility = View.GONE
        view.findViewById<LinearProgressIndicator>(R.id.qprogress).setProgress(0)
        showQuestion(view)
    }

    /** One question per skill band, random within the band for variety. */
    private fun pick4(): List<Q> = listOf(
        questions[Random.nextInt(2)],
        questions[2 + Random.nextInt(2)],
        questions[4],
        questions[5]
    )

    private fun showQuestion(view: View) {
        val q = active[qIndex]
        view.findViewById<LinearProgressIndicator>(R.id.qprogress).setProgress(qIndex)
        view.findViewById<TextView>(R.id.txt_question).text =
            "سؤال ${qIndex + 1} از ۴\n${q.prompt}"

        val answers = view.findViewById<LinearLayout>(R.id.answers)
        answers.removeAllViews()
        q.opts.forEachIndexed { idx, opt ->
            val btn = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = opt
                insetTop = 6
                insetBottom = 6
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
                setOnClickListener { answer(view, idx, this) }
            }
            answers.addView(btn)
        }
    }

    private fun answer(view: View, idx: Int, picked: MaterialButton) {
        val q = active[qIndex]
        picked.setTextColor(
            resources.getColor(
                if (idx == q.correct) android.R.color.holo_green_dark
                else android.R.color.holo_red_dark,
                requireContext().theme
            )
        )
        if (idx == q.correct) correctCount++
        qIndex++
        view.findViewById<LinearProgressIndicator>(R.id.qprogress).setProgress(qIndex)
        if (qIndex < 4) {
            view.postDelayed({ showQuestion(view) }, 550)
        } else {
            suggested = computeSuggestion()
            store.setPlacementDone(true)
            store.prefs().edit().putInt("placement_lesson", suggested).apply()
            store.touchToday()
            showResult(view, suggested, false)
            view.findViewById<MaterialButton>(R.id.btn_start_quiz).apply {
                visibility = View.VISIBLE
                setText(R.string.practice_again)
            }
        }
    }

    /** The hardest band passed cleanly decides the floor. */
    private fun computeSuggestion(): Int {
        var s = 1
        for (i in 0 until 4) {
            if (i < correctCount) {
                s = maxOf(s, active[i].lessonIfCorrect)
            }
        }
        return minOf(s, 10)
    }

    private fun showResult(view: View, lesson: Int, recap: Boolean) {
        view.findViewById<View>(R.id.card_result).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.txt_result).text = if (recap) {
            "شروع از درس $lesson (طبق تشخیص قبلی)"
        } else {
            "شروع از درس $lesson  ($correctCount از ۴ درست)"
        }
        view.findViewById<TextView>(R.id.txt_guide).text =
            "درس $lesson را باز کن، جمله‌ها را مرور کن و با تمرین و آزمون ادامه بده. " +
            "تقلب نکن — پیشرفت واقعی وقتی است که خودت جواب بدهی!"
        view.findViewById<MaterialButton>(R.id.btn_go_lesson).setOnClickListener {
            startActivity(BookletActivity.intent(requireContext(), lesson))
        }
    }
}
