package com.michel.deutschmacht.booklet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Store

/** Tab 3: embeds the lesson-entry list (last used lesson remembered). */
class BookletFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_plain_host, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = Store(requireContext())
        val lesson = store.prefs().getInt("last_lesson", 1)
        if (childFragmentManager.findFragmentById(R.id.plain_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.plain_container, LessonEntriesFragment.embedded(lesson))
                .commit()
        }
    }
}
