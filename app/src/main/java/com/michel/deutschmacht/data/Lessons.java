package com.michel.deutschmacht.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads lessons.json from assets. Duplicated booklet lines are intentionally kept. */
public final class Lessons {

    public static class Entry {
        public final String de;
        public final String fa;
        public final int lesson;   // 1-based lesson number
        public final int index;    // position inside the lesson

        Entry(String de, String fa, int lesson, int index) {
            this.de = de;
            this.fa = fa;
            this.lesson = lesson;
            this.index = index;
        }
    }

    public static class Lesson {
        public final int num;
        public final List<Entry> entries = new ArrayList<>();

        Lesson(int num) { this.num = num; }
    }

    private static List<Lesson> cache;

    private Lessons() {}

    public static synchronized List<Lesson> load(Context ctx) {
        if (cache != null) return cache;
        List<Lesson> out = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(ctx.getAssets().open("lessons.json"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONArray root = new JSONArray(sb.toString());
            for (int i = 0; i < root.length(); i++) {
                JSONObject lo = root.getJSONObject(i);
                Lesson l = new Lesson(lo.getInt("num"));
                JSONArray arr = lo.getJSONArray("entries");
                for (int j = 0; j < arr.length(); j++) {
                    JSONObject eo = arr.getJSONObject(j);
                    l.entries.add(new Entry(eo.getString("de"), eo.getString("fa"), l.num, j));
                }
                out.add(l);
            }
        } catch (Exception e) {
            // Never crash the app over data: leave an empty structure.
        }
        cache = out;
        return out;
    }

    public static Lesson lesson(Context ctx, int num) {
        for (Lesson l : load(ctx)) if (l.num == num) return l;
        return new Lesson(num);
    }

    public static int totalEntries(Context ctx) {
        int n = 0;
        for (Lesson l : load(ctx)) n += l.entries.size();
        return n;
    }

    public static List<Entry> flat(Context ctx) {
        List<Entry> all = new ArrayList<>();
        for (Lesson l : load(ctx)) all.addAll(l.entries);
        return all;
    }
}
