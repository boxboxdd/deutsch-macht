package com.michel.deutschmacht.booklet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.michel.deutschmacht.R;
import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Speaker;
import com.michel.deutschmacht.data.Store;

import java.util.ArrayList;
import java.util.List;

/**
 * Booklet: lesson entries with optional hidden German column (self-testing) and "hard for me" flag.
 * Used standalone (from Coach) and embedded in the Booklet tab.
 */
public class LessonEntriesFragment extends Fragment {

    private static final String ARG_LESSON = "lesson";
    private static final String ARG_EMBEDDED = "embedded";

    private Store store;
    private boolean hideDe = false;
    private int lessonNum = 1;

    public static LessonEntriesFragment newInstance(int lesson) {
        LessonEntriesFragment f = new LessonEntriesFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_LESSON, lesson);
        b.putBoolean(ARG_EMBEDDED, false);
        f.setArguments(b);
        return f;
    }

    public static LessonEntriesFragment embedded(int lesson) {
        LessonEntriesFragment f = newInstance(lesson);
        f.getArguments().putBoolean(ARG_EMBEDDED, true);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        lessonNum = getArguments() == null ? 1 : getArguments().getInt(ARG_LESSON, 1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_booklet, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        store = new Store(requireContext());
        boolean embedded = getArguments() != null && getArguments().getBoolean(ARG_EMBEDDED);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        ChipGroup chips = v.findViewById(R.id.lesson_chips);
        MaterialButtonToggleGroup toggle = v.findViewById(R.id.toggle_hide);
        RecyclerView recycler = v.findViewById(R.id.recycler);

        // In the standalone activity the toolbar owns the back arrow.
        if (!embedded && requireActivity() instanceof AppCompatActivity) {
            AppCompatActivity act = (AppCompatActivity) requireActivity();
            act.setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(x -> requireActivity().finish());
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }

        // One chip per lesson
        for (Lessons.Lesson l : Lessons.load(requireContext())) {
            Chip chip = new Chip(requireContext());
            chip.setText("درس " + l.num);
            chip.setCheckable(true);
            chip.setOnClickListener(x -> selectLesson(l.num, null));
            chips.addView(chip);
        }
        chips.setOnCheckedStateChangeListener((g, checked) -> {
            if (!checked.isEmpty()) {
                String t = ((Chip) g.getChildAt(g.indexOfChild(g.findViewById(checked.get(0))))).getText().toString();
                t = t.replace("درس ", "").trim();
                try { lessonNum = Integer.parseInt(t); } catch (Exception ignored) {}
                selectLesson(lessonNum, null);
            }
        });

        // Hide/show German toggle
        hideDe = store.prefs().getBoolean(Store.KEY_HIDE_DE, false);
        toggle.check(hideDe ? R.id.btn_hide_de : R.id.btn_show_de);
        toggle.addOnButtonCheckedListener((g, id, isSel) -> {
            if (isSel) {
                hideDe = id == R.id.btn_hide_de;
                store.prefs().edit().putBoolean(Store.KEY_HIDE_DE, hideDe).apply();
                refreshAdapter(recycler, null);
            }
        });

        // Select initial lesson chip
        Chip initial = (Chip) chips.getChildAt(lessonNum - 1);
        if (initial != null) initial.setChecked(true);
        selectLesson(lessonNum, recycler);
    }

    private void selectLesson(int num, RecyclerView recycler) {
        lessonNum = num;
        RecyclerView r = recycler != null ? recycler : requireView().findViewById(R.id.recycler);
        refreshAdapter(r, null);
        if (!isAdded()) return;
        Lessons.Lesson l = Lessons.lesson(requireContext(), num);
        MaterialToolbar tb = requireView().findViewById(R.id.toolbar);
        tb.setTitle("جزوه — درس " + num + " (" + l.entries.size() + ")");
    }

    private void refreshAdapter(RecyclerView recycler, List<Lessons.Entry> reuse) {
        if (!isAdded()) return;
        Lessons.Lesson l = Lessons.lesson(requireContext(), lessonNum);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new EntryAdapter(l));
    }

    private class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.H> {
        final Lessons.Lesson lesson;
        EntryAdapter(Lessons.Lesson l) { lesson = l; }

        @NonNull
        @Override
        public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new H(getLayoutInflater().inflate(R.layout.item_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull H h, int pos) {
            Lessons.Entry e = lesson.entries.get(pos);
            h.bind(e);
        }

        @Override
        public int getItemCount() { return lesson.entries.size(); }

        class H extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView de, fa, reveal;
            final ImageButton hard;

            H(View v) {
                super(v);
                card = (MaterialCardView) v;
                de = v.findViewById(R.id.txt_de);
                fa = v.findViewById(R.id.txt_fa);
                reveal = v.findViewById(R.id.txt_reveal);
                hard = v.findViewById(R.id.btn_hard);
            }

            void bind(Lessons.Entry e) {
                int pos = getBindingAdapterPosition();
                final int idx = pos < 0 ? e.index : pos;

                de.setText(e.de);
                fa.setText(e.fa);
                de.setVisibility(hideDe ? View.GONE : View.VISIBLE);
                reveal.setVisibility(hideDe ? View.VISIBLE : View.GONE);
                fa.setAlpha(hideDe ? 0.55f : 1f);

                boolean isHard = store.isHard(e.lesson, idx);
                hard.setAlpha(isHard ? 1f : 0.35f);

                card.setOnClickListener(x -> {
                    if (hideDe) {  // tap reveals the German (self-test)
                        de.setVisibility(View.VISIBLE);
                        reveal.setVisibility(View.GONE);
                        fa.setAlpha(1f);
                    }
                });

                card.findViewById(R.id.btn_speak).setOnClickListener(x -> Speaker.speak(e.de));

                hard.setOnClickListener(x -> {
                    boolean now = !store.isHard(e.lesson, idx);
                    store.setHard(e.lesson, idx, now);
                    hard.setAlpha(now ? 1f : 0.35f);
                });
            }
        }
    }
}
