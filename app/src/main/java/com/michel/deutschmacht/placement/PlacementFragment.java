package com.michel.deutschmacht.placement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.michel.deutschmacht.R;
import com.michel.deutschmacht.booklet.BookletActivity;
import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Tab 2: 4-question diagnostic quiz that recommends a starting lesson + guide. */
public class PlacementFragment extends Fragment {

    /** One question per skill band; answer index = correct option. */
    private static final class Q {
        final String prompt;
        final String[] opts;
        final int correct;
        final int lessonIfCorrect;
        Q(String p, String[] o, int c, int l) { prompt = p; opts = o; correct = c; lessonIfCorrect = l; }
    }

    private final Q[] QUESTIONS = {
        // Built from booklet entries; band 1 (L1-2)
        new Q("«ich will» یعنی چه؟",
                new String[]{"من می‌خواهم", "تو می‌خواهی", "ما می‌خواهیم", "آن‌ها می‌خواهند"}, 0, 1),
        new Q("«Wo ist es?» یعنی چه؟",
                new String[]{"این چیست؟", "کجاست؟", "چه زمانی؟", "چرا؟"}, 1, 2),
        // Band 2 (L3-6)
        new Q("جای خالی: «ich ___ nach Deutschland fahren» (می‌خواهم به آلمان بروم)",
                new String[]{"will", "möchte", "kann", "muss"}, 1, 5),
        new Q("«Ich kann es nicht finden» یعنی چه؟",
                new String[]{"من می‌توانم آن را ببینم", "من نمی‌توانم آن را پیدا کنم", "من آن را نمی‌خواهم", "من آن را نمی‌دانم"}, 1, 4),
        // One spare per band if needed
        new Q("«Sie müssen hier bleiben» یعنی چه؟",
                new String[]{"شما باید اینجا بمانید", "شما می‌خواهید اینجا بمانید", "شما اینجا هستید", "شما اینجا کار می‌کنید"}, 0, 6),
        new Q("«Ich habe es gekauft» یعنی چه؟",
                new String[]{"من آن را می‌خرم", "من آن را خریدم", "من آن را می‌خواهم", "من آن را خواهم خرید"}, 1, 8),
    };

    private List<Q> active;
    private int qIndex, correctCount, suggested = 1;
    private Store store;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_placement, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        store = new Store(requireContext());
        MaterialButton start = v.findViewById(R.id.btn_start_quiz);
        start.setOnClickListener(x -> startQuiz(v));

        if (store.placementDone()) {
            start.setText(R.string.practice_again);
            showResult(v, store.prefs().getInt("placement_lesson", 1), true);
        } else {
            ((TextView) v.findViewById(R.id.txt_question)).setText("۴ سؤال — آماده‌ای؟");
        }
    }

    private void startQuiz(View v) {
        active = pick4();
        qIndex = 0;
        correctCount = 0;
        v.findViewById(R.id.btn_start_quiz).setVisibility(View.GONE);
        v.findViewById(R.id.card_result).setVisibility(View.GONE);
        ((LinearProgressIndicator) v.findViewById(R.id.qprogress)).setProgress(0);
        showQuestion(v);
    }

    /** 4 questions: one per band, chosen deterministically-random for variety. */
    private List<Q> pick4() {
        Random r = new Random();
        List<Q> out = new ArrayList<>();
        out.add(QUESTIONS[r.nextInt(2)]);            // band 1
        out.add(QUESTIONS[2 + r.nextInt(2)]);        // band 2
        out.add(QUESTIONS[4]);                       // band 3
        out.add(QUESTIONS[5]);                       // band 3+
        return out;
    }

    private void showQuestion(View v) {
        Q q = active.get(qIndex);
        ((LinearProgressIndicator) v.findViewById(R.id.qprogress)).setProgress(qIndex);
        TextView txt = v.findViewById(R.id.txt_question);
        txt.setText("سؤال " + (qIndex + 1) + " از ۴\n" + q.prompt);

        LinearLayout answers = v.findViewById(R.id.answers);
        answers.removeAllViews();
        for (int i = 0; i < q.opts.length; i++) {
            final int idx = i;
            MaterialButton btn = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(q.opts[i]);
            btn.setInsetTop(6); btn.setInsetBottom(6);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = 8;
            btn.setLayoutParams(lp);
            btn.setOnClickListener(x -> answer(v, idx, btn));
            answers.addView(btn);
        }
    }

    private void answer(View v, int idx, MaterialButton picked) {
        Q q = active.get(qIndex);
        picked.setTextColor(getResources().getColor(idx == q.correct
                ? com.google.android.material.R.color.design_default_color_primary
                : com.google.android.material.R.color.design_default_color_error, requireContext().getTheme()));
        if (idx == q.correct) correctCount++;
        qIndex++;
        ((LinearProgressIndicator) v.findViewById(R.id.qprogress)).setProgress(qIndex);
        if (qIndex < 4) {
            v.postDelayed(() -> showQuestion(v), 550);
        } else {
            suggested = computeSuggestion();
            store.setPlacementDone(true);
            store.prefs().edit().putInt("placement_lesson", suggested).apply();
            store.touchToday();
            showResult(v, suggested, false);
            v.findViewById(R.id.btn_start_quiz).setVisibility(View.VISIBLE);
            ((MaterialButton) v.findViewById(R.id.btn_start_quiz)).setText(R.string.practice_again);
        }
    }

    private int computeSuggestion() {
        // The hardest band passed cleanly decides the floor.
        int s = 1;
        for (int i = 0; i < 4; i++) {
            if (i < correctCount) {
                int bandLesson = active.get(i).lessonIfCorrect;
                if (bandLesson > s) s = bandLesson;
            }
        }
        return Math.min(s, 10);
    }

    private void showResult(View v, int lesson, boolean recap) {
        v.findViewById(R.id.card_result).setVisibility(View.VISIBLE);
        TextView result = v.findViewById(R.id.txt_result);
        result.setText(recap
                ? "شروع از درس " + lesson + " (طبق تشخیص قبلی)"
                : "شروع از درس " + lesson + "  (" + correctCount + " از ۴ درست)");
        ((TextView) v.findViewById(R.id.txt_guide)).setText(
                "درس " + lesson + " را باز کن، جمله‌ها را مرور کن و با تمرین و آزمون ادامه بده. تقلب نکن — پیشرفت واقعی وقتی است که خودت جواب بدهی!");
        MaterialButton go = v.findViewById(R.id.btn_go_lesson);
        go.setOnClickListener(x -> startActivity(BookletActivity.intent(requireContext(), lesson)));
    }
}
