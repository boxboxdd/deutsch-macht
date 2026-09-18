package com.michel.deutschmacht.booklet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.data.Speaker
import com.michel.deutschmacht.data.Store

/**
 * Booklet: lesson entries with optional hidden German column (self-testing) and "hard for me" flag.
 * Used standalone (from Coach) and embedded in the Booklet tab.
 */
class LessonEntriesFragment : Fragment() {

    private lateinit var store: Store
    private var hideDe = false
    private var lessonNum = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonNum = arguments?.getInt(ARG_LESSON, 1) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_booklet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = Store(requireContext())
        val embedded = arguments?.getBoolean(ARG_EMBEDDED) ?: false

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val chips = view.findViewById<ChipGroup>(R.id.lesson_chips)
        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_hide)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)

        if (!embedded && requireActivity() is AppCompatActivity) {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
            toolbar.setNavigationOnClickListener { requireActivity().finish() }
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        }

        // One chip per lesson — the row scrolls horizontally so lessons 8/9/10 are reachable.
        for (l in Lessons.load(requireContext())) {
            val chip = Chip(requireContext()).apply {
                text = "درس ${l.num}"
                isCheckable = true
                setOnClickListener { selectLesson(l.num, recycler) }
            }
            chips.addView(chip)
        }
        chips.setOnCheckedStateChangeListener { group, checked ->
            if (checked.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checked[0])
                val num = chip.text.toString().replace("درس ", "").trim().toIntOrNull() ?: return@setOnCheckedStateChangeListener
                selectLesson(num, recycler)
            }
        }

        // Hide/show German toggle
        hideDe = store.prefs().getBoolean(Store.KEY_HIDE_DE, false)
        toggle.check(if (hideDe) R.id.btn_hide_de else R.id.btn_show_de)
        toggle.addOnButtonCheckedListener { _, id, isSel ->
            if (isSel) {
                hideDe = id == R.id.btn_hide_de
                store.prefs().edit().putBoolean(Store.KEY_HIDE_DE, hideDe).apply()
                refreshAdapter(recycler)
            }
        }

        // Select the initial lesson chip and scroll the row so it is visible.
        val initial = chips.getChildAt(lessonNum - 1) as? Chip
        initial?.isChecked = true
        view.findViewById<android.widget.HorizontalScrollView>(R.id.chips_scroll).post {
            val chipX = initial?.x ?: 0f
            view.findViewById<android.widget.HorizontalScrollView>(R.id.chips_scroll).smoothScrollTo(chipX.toInt(), 0)
        }
        selectLesson(lessonNum, recycler)
    }

    private fun selectLesson(num: Int, recycler: RecyclerView?) {
        lessonNum = num
        val r = recycler ?: return
        refreshAdapter(r)
        if (!isAdded) return
        val l = Lessons.lesson(requireContext(), num)
        view?.findViewById<MaterialToolbar>(R.id.toolbar)?.title = "جزوه — درس $num (${l.entries.size})"
    }

    private fun refreshAdapter(recycler: RecyclerView) {
        if (!isAdded) return
        val l = Lessons.lesson(requireContext(), lessonNum)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = EntryAdapter(l)
    }

    inner class EntryAdapter(private val lesson: Lessons.Lesson) :
        RecyclerView.Adapter<EntryAdapter.H>() {

        inner class H(v: View) : RecyclerView.ViewHolder(v) {
            val card: MaterialCardView = v as MaterialCardView
            val de: TextView = v.findViewById(R.id.txt_de)
            val fa: TextView = v.findViewById(R.id.txt_fa)
            val reveal: TextView = v.findViewById(R.id.txt_reveal)
            val hard: ImageButton = v.findViewById(R.id.btn_hard)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H =
            H(layoutInflater.inflate(R.layout.item_entry, parent, false))

        override fun getItemCount(): Int = lesson.entries.size

        override fun onBindViewHolder(holder: H, position: Int) {
            val e = lesson.entries[position]
            with(holder) {
                de.text = e.de
                fa.text = e.fa
                de.visibility = if (hideDe) View.GONE else View.VISIBLE
                reveal.visibility = if (hideDe) View.VISIBLE else View.GONE
                fa.alpha = if (hideDe) 0.55f else 1f

                hard.alpha = if (store.isHard(e.lesson, e.index)) 1f else 0.35f

                card.setOnClickListener {
                    if (hideDe) { // tap reveals the German (self-test)
                        de.visibility = View.VISIBLE
                        reveal.visibility = View.GONE
                        fa.alpha = 1f
                    }
                }

                card.findViewById<ImageButton>(R.id.btn_speak).setOnClickListener {
                    Speaker.speak(e.de)
                }

                hard.setOnClickListener {
                    val now = !store.isHard(e.lesson, e.index)
                    store.setHard(e.lesson, e.index, now)
                    hard.alpha = if (now) 1f else 0.35f
                }
            }
        }
    }

    companion object {
        private const val ARG_LESSON = "lesson"
        private const val ARG_EMBEDDED = "embedded"

        fun newInstance(lesson: Int): LessonEntriesFragment =
            LessonEntriesFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_LESSON, lesson)
                    putBoolean(ARG_EMBEDDED, false)
                }
            }

        fun embedded(lesson: Int): LessonEntriesFragment =
            newInstance(lesson).apply {
                arguments?.putBoolean(ARG_EMBEDDED, true)
            }
    }
}
