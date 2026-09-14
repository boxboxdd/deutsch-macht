package com.michel.deutschmacht.progress;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.michel.deutschmacht.R;
import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Store;

import java.util.List;

/** Tab 5: stats + per-lesson bars + 30-day calendar + badges. */
public class ProgressFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_progress, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        Store store = new Store(requireContext());
        List<Lessons.Lesson> lessons = Lessons.load(requireContext());

        int total = Lessons.totalEntries(requireContext());
        int learned = store.countLearnedAll(requireContext());

        ((TextView) v.findViewById(R.id.stat_learned)).setText(learned + " / " + total);
        ((TextView) v.findViewById(R.id.stat_quiz)).setText(String.valueOf(store.quizTotal()));
        ((TextView) v.findViewById(R.id.stat_hard)).setText(String.valueOf(store.hardCount(requireContext())));

        // Per-lesson rows
        LinearLayout box = v.findViewById(R.id.lesson_progress_box);
        for (Lessons.Lesson l : lessons) {
            int n = l.entries.size();
            int c2 = store.countLearned(l.num, l);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 12, 0, 12);

            TextView title = new TextView(requireContext());
            title.setText("درس " + l.num + " — " + c2 + " / " + n);
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setTextColor(0xFF1B4D3E);
            row.addView(title);

            LinearProgressIndicator bar = new LinearProgressIndicator(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = 6;
            bar.setProgress(n == 0 ? 0 : Math.round(c2 * 100f / n));
            bar.setMax(100);
            bar.setTrackThickness(8);
            bar.setTrackCornerRadius(4);
            bar.setLayoutParams(lp);
            row.addView(bar);

            box.addView(row);
        }

        CalendarView30 cal = v.findViewById(R.id.calendar);
        cal.refresh(store);

        BadgesView badges = v.findViewById(R.id.badges);
        badges.refresh(store, total, requireContext());
    }
}
