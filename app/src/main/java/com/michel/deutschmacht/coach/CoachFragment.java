package com.michel.deutschmacht.coach;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.michel.deutschmacht.R;
import com.michel.deutschmacht.booklet.BookletActivity;
import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Store;

/** Tab 1: hero + progress ring + no-cheating commitment + daily checklist + lesson picker. */
public class CoachFragment extends Fragment {

    private Store store;
    private int pickedLesson = 1;

    private final int[] checkIds = {R.id.chk1, R.id.chk2, R.id.chk3, R.id.chk4, R.id.chk5};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_coach, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        store = new Store(requireContext());

        // Ring: overall learned / total
        int total = Lessons.totalEntries(requireContext());
        int learned = store.countLearnedAll(requireContext());
        int pct = total == 0 ? 0 : Math.round(learned * 100f / total);
        ((com.google.android.material.progressindicator.CircularProgressIndicator)
                v.findViewById(R.id.ring_progress)).setProgress(pct);
        TextView ringTxt = v.findViewById(R.id.txt_ring_pct);
        ringTxt.setText(pct + "%");
        TextView streak = v.findViewById(R.id.txt_streak);
        streak.setText("🔥 " + store.streak() + " " + getString(R.string.streak_days)
                + "  •  " + learned + " " + getString(R.string.learned_words));

        // Commitment
        TextView saved = v.findViewById(R.id.txt_commit_saved);
        TextInputLayout til = v.findViewById(R.id.til_commit);
        TextInputEditText edt = v.findViewById(R.id.edt_commit);
        MaterialButton btn = v.findViewById(R.id.btn_commit);
        String existing = store.commitment();
        if (existing != null) {
            saved.setVisibility(View.VISIBLE);
            saved.setText(getString(R.string.commitment_saved) + "\n«" + existing + "»");
            til.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
        }
        btn.setOnClickListener(x -> {
            String t = edt.getText() == null ? null : edt.getText().toString().trim();
            if (t == null || t.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.commitment_hint), Toast.LENGTH_SHORT).show();
                return;
            }
            store.setCommitment(t);
            saved.setVisibility(View.VISIBLE);
            saved.setText(getString(R.string.commitment_saved) + "\n«" + t + "»");
            til.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
        });

        // Checklist
        boolean[] done = store.checklistDone();
        for (int i = 0; i < checkIds.length; i++) {
            CheckBox cb = v.findViewById(checkIds[i]);
            final int idx = i;
            cb.setChecked(done[i]);
            cb.setOnCheckedChangeListener((bv, is) -> store.setChecklist(idx, is));
        }

        // Lesson picker
        Slider slider = v.findViewById(R.id.slider_lesson);
        TextView pick = v.findViewById(R.id.txt_lesson_pick);
        TextView stat = v.findViewById(R.id.txt_lesson_stat);
        pickedLesson = Math.max(1, Math.min(10, store.prefs().getInt("last_lesson", 1)));
        slider.setValue(pickedLesson);
        refreshPick(pick, stat);
        slider.addOnChangeListener((s, val, fromUser) -> {
            pickedLesson = (int) val;
            store.prefs().edit().putInt("last_lesson", pickedLesson).apply();
            refreshPick(pick, stat);
        });

        MaterialButton start = v.findViewById(R.id.btn_start_lesson);
        start.setOnClickListener(x -> {
            Lessons.Lesson l = Lessons.lesson(requireContext(), pickedLesson);
            if (l.entries.isEmpty()) return;
            startActivity(BookletActivity.intent(requireContext(), pickedLesson));
        });

        MaterialButton quiz = v.findViewById(R.id.btn_quiz_lesson);
        quiz.setOnClickListener(x -> startActivity(
                new android.content.Intent(requireContext(), com.michel.deutschmacht.booklet.QuizActivity.class)
                        .putExtra("lesson", pickedLesson)));
    }

    private void refreshPick(TextView pick, TextView stat) {
        Lessons.Lesson l = Lessons.lesson(requireContext(), pickedLesson);
        pick.setText(getString(R.string.start_lesson) + " — درس " + pickedLesson + " (" + l.entries.size() + " جمله)");
        int learned = store.countLearned(pickedLesson, l);
        stat.setText(learned + " / " + l.entries.size() + " " + getString(R.string.learned_words));
    }
}
