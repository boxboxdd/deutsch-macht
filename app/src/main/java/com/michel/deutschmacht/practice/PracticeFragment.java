package com.michel.deutschmacht.practice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.michel.deutschmacht.R;
import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Speaker;
import com.michel.deutschmacht.data.Store;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Tab 4: mic record/play + German TTS + sentence scramble + spaced repetition. */
public class PracticeFragment extends Fragment {

    private static final int REQ_MIC = 42;

    private Store store;
    private final Recorder recorder = new Recorder();
    private final Random rnd = new Random();

    // Current entry shared by the TTS/recorder card and the scramble game
    private Lessons.Entry current;
    private List<Lessons.Entry> scrambleDeck = new ArrayList<>();

    // scramble state
    private final List<String> answer = new ArrayList<>();
    private final List<Integer> poolOrder = new ArrayList<>();
    private String[] poolTokens;

    // SR card
    private Lessons.Entry srEntry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_practice, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        store = new Store(requireContext());

        // --- sentence card ---
        pickRandomSentence();
        MaterialButton tts = v.findViewById(R.id.btn_tts);
        tts.setOnClickListener(x -> { if (current != null) Speaker.speak(current.de); });

        MaterialButton rec = v.findViewById(R.id.btn_rec);
        rec.setOnClickListener(x -> toggleRec(v));

        MaterialButton play = v.findViewById(R.id.btn_play);
        play.setOnClickListener(x -> {
            String f = recorder.lastFile();
            if (f == null) {
                Toast.makeText(getContext(), R.string.practice_record, Toast.LENGTH_SHORT).show();
                return;
            }
            try { recorder.play(f); } catch (Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // --- scramble ---
        newScrambleRound(v);
        v.findViewById(R.id.btn_scramble_check).setOnClickListener(x -> checkScramble(v));
        v.findViewById(R.id.btn_scramble_next).setOnClickListener(x -> newScrambleRound(v));

        // --- SR ---
        loadSr(v);
        v.findViewById(R.id.btn_sr_reveal).setOnClickListener(x -> {
            if (srEntry == null) return;
            TextView fa = v.findViewById(R.id.txt_sr_fa);
            fa.setVisibility(View.VISIBLE);
            Speaker.speak(srEntry.de);
        });
        v.findViewById(R.id.btn_sr_known).setOnClickListener(x -> srAnswer(v, true));
        v.findViewById(R.id.btn_sr_dontknow).setOnClickListener(x -> srAnswer(v, false));
    }

    // ===== sentence + recorder =====

    private void pickRandomSentence() {
        List<Lessons.Entry> all = Lessons.flat(requireContext());
        if (all.isEmpty()) return;
        current = all.get(rnd.nextInt(all.size()));
        ((TextView) requireView().findViewById(R.id.txt_sentence)).setText(current.de);
        ((TextView) requireView().findViewById(R.id.txt_sentence_fa)).setText(current.fa);
    }

    private void toggleRec(View v) {
        MaterialButton rec = v.findViewById(R.id.btn_rec);
        if (recorder.isRecording()) {
            recorder.stopRecording();
            rec.setText(R.string.practice_record);
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        try {
            recorder.start(requireContext());
            rec.setText(R.string.practice_stop);
            Toast.makeText(getContext(), R.string.practice_record, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] g) {
        if (code == REQ_MIC && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED && isAdded()) {
            toggleRec(requireView());
        }
    }

    // ===== scramble game =====

    private void newScrambleRound(View v) {
        if (scrambleDeck.isEmpty()) {
            scrambleDeck = new ArrayList<>(Lessons.flat(requireContext()));
            Collections.shuffle(scrambleDeck);
        }
        Lessons.Entry e = scrambleDeck.remove(scrambleDeck.size() - 1);
        current = e;
        ((TextView) v.findViewById(R.id.txt_sentence)).setText(e.de);
        ((TextView) v.findViewById(R.id.txt_sentence_fa)).setText(e.fa);

        poolTokens = e.de.split("\\s+");
        poolOrder.clear();
        for (int i = 0; i < poolTokens.length; i++) poolOrder.add(i);
        Collections.shuffle(poolOrder, rnd);
        if (poolOrder.equals(order0To(poolTokens.length))) Collections.reverse(poolOrder);
        answer.clear();
        renderScramble(v, "");
        ((TextView) v.findViewById(R.id.txt_scramble_state)).setText("");
    }

    /** identity list helper — used to un-shuffle trivial orderings */
    private static List<Integer> order0To(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(i);
        return l;
    }

    private void renderScramble(View v, String stateMsg) {
        LinearLayout ansRow = v.findViewById(R.id.scramble_answer);
        LinearLayout poolRow = v.findViewById(R.id.scramble_pool);
        ansRow.removeAllViews();
        poolRow.removeAllViews();

        for (int pos = 0; pos < answer.size(); pos++) {
            final int slot = pos;
            MaterialButton b = chip(answer.get(slot));
            b.setOnClickListener(x -> {          // tap answer token -> back to pool
                answer.remove(slot);
                renderScramble(v, "");
            });
            ansRow.addView(b);
        }
        for (int i = 0; i < poolOrder.size(); i++) {
            final int pi = i;
            MaterialButton b = chip(poolTokens[poolOrder.get(pi)]);
            b.setOnClickListener(x -> {
                answer.add(poolTokens[poolOrder.get(pi)]);
                poolOrder.remove(pi);
                renderScramble(v, "");
            });
            poolRow.addView(b);
        }
        if (stateMsg != null && !stateMsg.isEmpty()) {
            ((TextView) v.findViewById(R.id.txt_scramble_state)).setText(stateMsg);
        }
    }

    private MaterialButton chip(String text) {
        MaterialButton b = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(text);
        b.setInsetTop(4);
        b.setInsetBottom(4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 8, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private void checkScramble(View v) {
        if (current == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < answer.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(answer.get(i));
        }
        boolean ok = sb.toString().trim().equals(current.de.trim());
        ((TextView) v.findViewById(R.id.txt_scramble_state)).setText(ok
                ? getString(R.string.practice_correct)
                : getString(R.string.practice_wrong, current.de));
        if (ok) {
            store.touchToday();
            Speaker.speak(current.de);
        }
    }

    // ===== spaced repetition =====

    private void loadSr(View v) {
        List<Lessons.Entry> due = store.srDueEntries(requireContext(), null, 50);
        if (due.isEmpty()) {
            srEntry = null;
            ((TextView) v.findViewById(R.id.txt_sr_de)).setText(R.string.practice_no_due);
            ((TextView) v.findViewById(R.id.txt_sr_fa)).setText("");
            v.findViewById(R.id.txt_sr_fa).setVisibility(View.GONE);
            ((TextView) v.findViewById(R.id.txt_sr_state)).setText("");
            return;
        }
        srEntry = due.get(rnd.nextInt(Math.min(due.size(), 10)));
        ((TextView) v.findViewById(R.id.txt_sr_de)).setText(srEntry.de);
        TextView fa = v.findViewById(R.id.txt_sr_fa);
        fa.setText(srEntry.fa);
        fa.setVisibility(View.GONE);
        int box = store.srBox(srEntry.lesson, srEntry.index);
        String dueIn = store.srDue(srEntry.lesson, srEntry.index) <= System.currentTimeMillis()
                ? "الان" : new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(store.srDue(srEntry.lesson, srEntry.index)));
        ((TextView) v.findViewById(R.id.txt_sr_state)).setText(String.format(getString(R.string.sr_due_in), box + 1, dueIn));
    }

    private void srAnswer(View v, boolean known) {
        if (srEntry == null) return;
        if (known) {
            store.srCorrect(srEntry.lesson, srEntry.index);
            store.setLearned(srEntry.lesson, srEntry.index, true);
        } else {
            store.srWrong(srEntry.lesson, srEntry.index);
        }
        store.srSchedule(srEntry.lesson, srEntry.index);
        store.touchToday();
        loadSr(v);
    }

    @Override
    public void onPause() {
        super.onPause();
        recorder.stopAll();
    }
}
