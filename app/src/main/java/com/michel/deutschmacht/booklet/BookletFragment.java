package com.michel.deutschmacht.booklet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.michel.deutschmacht.R;
import com.michel.deutschmacht.data.Store;

/** Tab 3: embeds the lesson-entry list (last used lesson remembered). */
public class BookletFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_plain_host, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        Store store = new Store(requireContext());
        int lesson = store.prefs().getInt("last_lesson", 1);
        if (getChildFragmentManager().findFragmentById(R.id.plain_container) == null) {
            FragmentTransaction t = getChildFragmentManager().beginTransaction()
                    .replace(R.id.plain_container, LessonEntriesFragment.embedded(lesson));
            t.commit();
        }
    }
}
