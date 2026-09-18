package com.michel.deutschmacht.practice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.Speaker
import com.michel.deutschmacht.data.Store
import com.michel.deutschmacht.ui.FlowLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Tab 4: mic record/play + German TTS + sentence scramble + spaced repetition. */
class PracticeFragment : Fragment() {

    private lateinit var store: Store
    private val recorder = Recorder()

    private var current: Lessons.Entry? = null
    private var scrambleDeck = mutableListOf<Lessons.Entry>()

    private val answer = mutableListOf<String>()
    private val poolOrder = mutableListOf<Int>()
    private var poolTokens = emptyArray<String>()

    private var srEntry: Lessons.Entry? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_practice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = Store(requireContext())
        setupSentenceCard(view)
        setupScramble(view)
        setupSpacedRepetition(view)
    }

    private fun setupSentenceCard(view: View) {
        pickRandomSentence()
        view.findViewById<MaterialButton>(R.id.btn_tts).setOnClickListener {
            current?.let { Speaker.speak(it.de) }
        }
        view.findViewById<MaterialButton>(R.id.btn_rec).setOnClickListener { toggleRec(view) }
        view.findViewById<MaterialButton>(R.id.btn_play).setOnClickListener {
            val f = recorder.lastFile()
            if (f == null) {
                Toast.makeText(context, R.string.practice_record, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            runCatching { recorder.play(f) }.onFailure {
                Toast.makeText(context, it.message ?: "", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickRandomSentence() {
        val all = Lessons.flat(requireContext())
        if (all.isEmpty()) return
        current = all[Random.nextInt(all.size)]
        view?.findViewById<TextView>(R.id.txt_sentence)?.text = current?.de
        view?.findViewById<TextView>(R.id.txt_sentence_fa)?.text = current?.fa
    }

    private fun toggleRec(view: View) {
        val rec = view.findViewById<MaterialButton>(R.id.btn_rec)
        if (recorder.isRecording) {
            recorder.stopRecording()
            rec.setText(R.string.practice_record)
            return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC
            )
            return
        }
        runCatching { recorder.start(requireContext()) }
            .onSuccess {
                rec.setText(R.string.practice_stop)
                Toast.makeText(context, R.string.practice_record, Toast.LENGTH_SHORT).show()
            }
            .onFailure { Toast.makeText(context, it.message ?: "", Toast.LENGTH_SHORT).show() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED && isAdded
        ) {
            toggleRec(requireView())
        }
    }

    private fun setupScramble(view: View) {
        newScrambleRound(view)
        view.findViewById<MaterialButton>(R.id.btn_scramble_check).setOnClickListener {
            checkScramble(view)
        }
        view.findViewById<MaterialButton>(R.id.btn_scramble_next).setOnClickListener {
            newScrambleRound(view)
        }
    }

    private fun newScrambleRound(view: View) {
        if (scrambleDeck.isEmpty()) {
            scrambleDeck = Lessons.flat(requireContext()).toMutableList()
            scrambleDeck.shuffle()
        }
        val e = scrambleDeck.removeAt(scrambleDeck.size - 1)
        current = e
        view.findViewById<TextView>(R.id.txt_sentence).text = e.de
        view.findViewById<TextView>(R.id.txt_sentence_fa).text = e.fa
        view.findViewById<TextView>(R.id.txt_scramble_prompt).text =
            getString(R.string.scramble_prompt, e.fa)
        view.findViewById<TextView>(R.id.scramble_hint).visibility = View.GONE

        poolTokens = e.de.split(Regex("\\s+")).toTypedArray()
        poolOrder.clear()
        for (i in poolTokens.indices) poolOrder.add(i)
        poolOrder.shuffle()
        if (poolOrder == poolTokens.indices.toList()) poolOrder.reverse()
        answer.clear()
        renderScramble(view)
        view.findViewById<TextView>(R.id.txt_scramble_state).text = ""
    }

    private fun renderScramble(view: View) {
        val ansRow = view.findViewById<FlowLayout>(R.id.scramble_answer)
        val poolRow = view.findViewById<FlowLayout>(R.id.scramble_pool)
        ansRow.removeAllViews()
        poolRow.removeAllViews()

        for (pos in answer.indices) {
            val token = answer[pos]
            val b = chip(token)
            b.setOnClickListener { answer.removeAt(pos); renderScramble(view) }
            ansRow.addView(b)
        }
        for (i in poolOrder.indices) {
            val token = poolTokens[poolOrder[i]]
            val b = chip(token)
            b.setOnClickListener { answer.add(token); poolOrder.removeAt(i); renderScramble(view) }
            poolRow.addView(b)
        }
    }

    private fun chip(text: String): MaterialButton = MaterialButton(
        requireContext(), null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        setText(text)
        insetTop = 4
        insetBottom = 4
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun checkScramble(view: View) {
        val e = current ?: return
        val built = answer.joinToString(" ")
        val ok = built.trim() == e.de.trim()
        val state = view.findViewById<TextView>(R.id.txt_scramble_state)
        val hint = view.findViewById<TextView>(R.id.scramble_hint)
        if (ok) {
            state.text = getString(R.string.practice_correct)
            state.setTextColor(requireContext().getColor(R.color.green))
            hint.visibility = View.GONE
            store.setLearned(e.lesson, e.index, true)
            store.srCorrect(e.lesson, e.index)
            store.srSchedule(e.lesson, e.index)
            Speaker.speak(e.de)
        } else {
            state.text = getString(R.string.practice_wrong, e.de)
            state.setTextColor(requireContext().getColor(R.color.red_light))
            hint.visibility = View.VISIBLE
            hint.text = getString(R.string.scramble_hint)
            store.srWrong(e.lesson, e.index)
            store.srSchedule(e.lesson, e.index)
        }
        store.touchToday()
    }

    private fun setupSpacedRepetition(view: View) {
        loadSr(view)
        view.findViewById<MaterialButton>(R.id.btn_sr_reveal).setOnClickListener {
            val e = srEntry ?: return@setOnClickListener
            view.findViewById<TextView>(R.id.txt_sr_fa).visibility = View.VISIBLE
            Speaker.speak(e.de)
        }
        view.findViewById<MaterialButton>(R.id.btn_sr_known).setOnClickListener { srAnswer(view, true) }
        view.findViewById<MaterialButton>(R.id.btn_sr_dontknow).setOnClickListener { srAnswer(view, false) }
    }

    private fun loadSr(view: View) {
        val due = store.srDueEntries(requireContext(), null, 50)
        if (due.isEmpty()) {
            srEntry = null
            view.findViewById<TextView>(R.id.txt_sr_de).setText(R.string.practice_no_due)
            view.findViewById<TextView>(R.id.txt_sr_fa).text = ""
            view.findViewById<TextView>(R.id.txt_sr_fa).visibility = View.GONE
            view.findViewById<TextView>(R.id.txt_sr_state).text = ""
            return
        }
        srEntry = due[Random.nextInt(minOf(due.size, 10))]
        val e = srEntry!!
        view.findViewById<TextView>(R.id.txt_sr_de).text = e.de
        view.findViewById<TextView>(R.id.txt_sr_fa).text = e.fa
        view.findViewById<TextView>(R.id.txt_sr_fa).visibility = View.GONE
        val dueAt = store.srDue(e.lesson, e.index)
        val dueIn = if (dueAt <= System.currentTimeMillis()) "الان"
            else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueAt))
        view.findViewById<TextView>(R.id.txt_sr_state).text =
            getString(R.string.sr_due_in, store.srBox(e.lesson, e.index) + 1, dueIn)
    }

    private fun srAnswer(view: View, known: Boolean) {
        val e = srEntry ?: return
        if (known) {
            store.srCorrect(e.lesson, e.index)
            store.setLearned(e.lesson, e.index, true)
        } else {
            store.srWrong(e.lesson, e.index)
        }
        store.srSchedule(e.lesson, e.index)
        store.touchToday()
        loadSr(view)
    }

    override fun onPause() {
        super.onPause()
        recorder.stopAll()
    }

    companion object {
        private const val REQ_MIC = 42
    }
}
