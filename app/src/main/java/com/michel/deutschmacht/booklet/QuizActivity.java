package com.michel.deutschmacht.booklet;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.michel.deutschmacht.R;
import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Short per-lesson quiz (8 questions, 4 options, both directions). */
public class QuizActivity extends AppCompatActivity {

    private static final class Q {
        final String prompt, correct;
        final List<String> options = new ArrayList<>();
        Q(String p, String c, List<String> o) { prompt = p; correct = c; options.addAll(o); }
    }

    private List<Q> questions;
    private int qi, score;
    private Store store;
    private int lesson;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_quiz);
        store = new Store(this);
        lesson = getIntent().getIntExtra("lesson", 1);
        questions = build(lesson);
        qi = 0;
        score = 0;
        show();
    }

    private List<Q> build(int lessonNum) {
        Lessons.Lesson l = Lessons.lesson(this, lessonNum);
        List<Lessons.Entry> all = Lessons.flat(this);
        Random r = new Random();
        List<Integer> picks = new ArrayList<>();
        for (int i = 0; i < l.entries.size(); i++) picks.add(i);
        Collections.shuffle(picks, r);
        int n = Math.min(8, picks.size());
        List<Q> out = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            Lessons.Entry e = l.entries.get(picks.get(k));
            boolean deToFa = r.nextBoolean();
            String prompt = deToFa ? e.de : e.fa;
            String correct = deToFa ? e.fa : e.de;
            List<String> opts = new ArrayList<>();
            opts.add(correct);
            int guard = 0;
            while (opts.size() < 4 && guard++ < 200) {
                Lessons.Entry other = all.get(r.nextInt(all.size()));
                String cand = deToFa ? other.fa : other.de;
                if (!opts.contains(cand)) opts.add(cand);
            }
            Collections.shuffle(opts, r);
            out.add(new Q(prompt, correct, opts));
        }
        return out;
    }

    private void show() {
        TextView title = findViewById(R.id.txt_quiz_title);
        TextView progress = findViewById(R.id.txt_quiz_progress);
        TextView prompt = findViewById(R.id.txt_quiz_prompt);
        LinearLayout opts = findViewById(R.id.quiz_options);
        findViewById(R.id.txt_quiz_result).setVisibility(View.GONE);
        findViewById(R.id.btn_quiz_done).setVisibility(View.GONE);

        if (qi >= questions.size()) { finishQuiz(); return; }
        title.setText("آزمون — درس " + lesson);
        progress.setText((qi + 1) + " / " + questions.size() + "  •  امتیاز: " + score);
        Q q = questions.get(qi);
        prompt.setText(q.prompt);
        opts.removeAllViews();
        for (String o : q.options) {
            MaterialButton btn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(o);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = 10;
            btn.setLayoutParams(lp);
            btn.setOnClickListener(x -> {
                boolean ok = o.equals(q.correct);
                btn.setBackgroundColor(getResources().getColor(ok
                        ? android.R.color.holo_green_light
                        : android.R.color.holo_red_light, getTheme()));
                if (ok) score++;
                opts.setEnabled(false);
                for (int i = 0; i < opts.getChildCount(); i++) opts.getChildAt(i).setEnabled(false);
                findViewById(R.id.quiz_options).postDelayed(() -> {
                    qi++;
                    show();
                }, 600);
            });
            opts.addView(btn);
        }
    }

    private void finishQuiz() {
        int pct = questions.isEmpty() ? 0 : Math.round(score * 100f / questions.size());
        store.saveScore(lesson, pct);
        ((TextView) findViewById(R.id.txt_quiz_progress)).setText("");
        ((TextView) findViewById(R.id.txt_quiz_prompt)).setText("");
        LinearLayout opts = findViewById(R.id.quiz_options);
        opts.removeAllViews();
        findViewById(R.id.txt_quiz_result).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txt_quiz_result)).setText("نتیجه: " + score + " از " + questions.size()
                + "  (" + pct + "٪)");
        MaterialButton done = findViewById(R.id.btn_quiz_done);
        done.setVisibility(View.VISIBLE);
        done.setOnClickListener(x -> finish());
        Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show();
    }
}
