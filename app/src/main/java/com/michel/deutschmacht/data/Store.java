package com.michel.deutschmacht.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/** All persistent state: learned/hard flags, quiz scores, daily activity, streak, badges, settings. */
public final class Store {

    private static final String NAME = "deutsch_macht";

    public static final String KEY_HIDE_DE = "hide_german";
    public static final String KEY_COMMIT = "commitment";
    public static final String KEY_PLACEMENT_DONE = "placement_done";

    private final SharedPreferences p;

    public Store(Context c) { p = c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE); }

    /** Raw prefs access for simple key/value extras (e.g. last chosen lesson). */
    public SharedPreferences prefs() { return p; }

    private static String day() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    // ---- per-entry flags ----

    public boolean isLearned(int lesson, int index) { return p.getBoolean("l" + lesson + "_" + index, false); }

    public void setLearned(int lesson, int index, boolean v) {
        p.edit().putBoolean("l" + lesson + "_" + index, v).apply();
        if (v) touchToday();
    }

    public boolean isHard(int lesson, int index) { return p.getBoolean("h" + lesson + "_" + index, false); }

    public void setHard(int lesson, int index, boolean v) { p.edit().putBoolean("h" + lesson + "_" + index, v).apply(); }

    public int countLearned(int lesson, com.michel.deutschmacht.data.Lessons.Lesson obj) {
        if (obj == null) return 0;
        int n = obj.entries.size();
        int c = 0;
        for (int i = 0; i < n; i++) if (isLearned(lesson, i)) c++;
        return c;
    }

    public int countLearnedAll(Context ctx) {
        int c = 0;
        for (com.michel.deutschmacht.data.Lessons.Lesson l : com.michel.deutschmacht.data.Lessons.load(ctx))
            c += countLearned(l.num, l);
        return c;
    }

    // ---- quiz scores ----

    public int bestScore(int lesson) { return p.getInt("q" + lesson, -1); }

    public void saveScore(int lesson, int score) {
        if (score > bestScore(lesson)) p.edit().putInt("q" + lesson, score).apply();
        touchToday();
    }

    // ---- daily activity (for the 30-day calendar + streak) ----

    /** Records activity (learning something / practice) for today. */
    public void touchToday() {
        String d = day();
        Set<String> days = new HashSet<>(p.getStringSet("days", new HashSet<>()));
        days.add(d);
        p.edit().putStringSet("days", days).apply();
    }

    /** Returns yyyy-mm-dd -> activity-count map for the last 30 days (today = last slot). */
    public String[] activeDays() { return p.getStringSet("days", new HashSet<>()).toArray(new String[0]); }

    public boolean isActiveToday() {
        String d = day();
        for (String s : activeDays()) if (s.equals(d)) return true;
        return false;
    }

    public int streak() {
        Set<String> days = p.getStringSet("days", new HashSet<>());
        int streak = 0;
        Calendar c = Calendar.getInstance();
        // If today has no activity yet, start counting from yesterday (streak not broken until midnight).
        if (!days.contains(day())) c.add(Calendar.DAY_OF_MONTH, -1);
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        for (int i = 0; i < 3650; i++) {
            String d = f.format(c.getTime());
            if (days.contains(d)) { streak++; c.add(Calendar.DAY_OF_MONTH, -1); }
            else break;
        }
        return streak;
    }

    // ---- daily checklist (5 items) ----

    private static final String CHECK_DONE = "check_done_"; // + day

    public boolean[] checklistDone() {
        String d = day();
        boolean[] out = new boolean[5];
        Set<String> s = p.getStringSet(CHECK_DONE + d, new HashSet<>());
        for (String v : s) {
            try { out[Integer.parseInt(v)] = true; } catch (Exception ignored) {}
        }
        return out;
    }

    public void setChecklist(int i, boolean v) {
        String d = day();
        Set<String> s = new HashSet<>(p.getStringSet(CHECK_DONE + d, new HashSet<>()));
        if (v) s.add(String.valueOf(i)); else s.remove(String.valueOf(i));
        p.edit().putStringSet(CHECK_DONE + d, s).apply();
        if (v) touchToday();
    }

    // ---- commitment card ("cheating forbidden") ----

    public String commitment() { return p.getString(KEY_COMMIT, null); }

    public void setCommitment(String text) { p.edit().putString(KEY_COMMIT, text).apply(); }

    // ---- placement ----

    public boolean placementDone() { return p.getBoolean(KEY_PLACEMENT_DONE, false); }

    public void setPlacementDone(boolean v) { p.edit().putBoolean(KEY_PLACEMENT_DONE, v).apply(); }

    // ---- spaced repetition (SR box per entry) ----

    public int srBox(int lesson, int index) { return p.getInt("sr" + lesson + "_" + index, 0); }

    public void srCorrect(int lesson, int index) {
        p.edit().putInt("sr" + lesson + "_" + index, Math.min(srBox(lesson, index) + 1, 5)).apply();
    }

    public void srWrong(int lesson, int index) {
        p.edit().putInt("sr" + lesson + "_" + index, 0).apply();
    }

    public long srDue(int lesson, int index) { return p.getLong("srd" + lesson + "_" + index, 0L); }

    public void srSchedule(int lesson, int index) {
        long[] gaps = {0, 1000L * 60 * 10, 1000L * 60 * 60, 1000L * 60 * 60 * 8,
                1000L * 60 * 60 * 24, 1000L * 60 * 60 * 24 * 3};
        long due = System.currentTimeMillis() + gaps[Math.min(srBox(lesson, index), gaps.length - 1)];
        p.edit().putLong("srd" + lesson + "_" + index, due).apply();
    }

    /** Entries currently due for review, from the chosen lesson (or all lessons). */
    public java.util.List<com.michel.deutschmacht.data.Lessons.Entry> srDueEntries(
            Context ctx, Integer onlyLesson, int limit) {
        java.util.List<com.michel.deutschmacht.data.Lessons.Entry> out = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (com.michel.deutschmacht.data.Lessons.Lesson l : com.michel.deutschmacht.data.Lessons.load(ctx)) {
            if (onlyLesson != null && l.num != onlyLesson) continue;
            for (com.michel.deutschmacht.data.Lessons.Entry e : l.entries) {
                if (srBox(e.lesson, e.index) > 0 && srDue(e.lesson, e.index) <= now) out.add(e);
            }
        }
        if (out.size() > limit) out = out.subList(out.size() - limit, out.size());
        return out;
    }

    // ---- stats for the Progress tab ----

    public int dueCount(Context ctx, Integer onlyLesson) {
        java.util.List<com.michel.deutschmacht.data.Lessons.Entry> l = srDueEntries(ctx, onlyLesson, Integer.MAX_VALUE);
        return l.size();
    }

    public int quizTotal() {
        int n = 0;
        for (String k : p.getAll().keySet()) if (k.startsWith("q")) n++;
        return n;
    }

    public int hardCount(Context ctx) {
        int n = 0;
        for (String k : p.getAll().keySet()) if (k.startsWith("h") && p.getBoolean(k, false)) n++;
        return n;
    }
}
