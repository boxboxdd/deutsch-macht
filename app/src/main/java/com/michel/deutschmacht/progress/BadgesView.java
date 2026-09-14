package com.michel.deutschmacht.progress;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.michel.deutschmacht.data.Lessons;
import com.michel.deutschmacht.data.Store;

import java.util.ArrayList;
import java.util.List;

/** Badge shelf drawn on canvas: locked badges are dim, earned badges full-color with emoji glyph. */
public class BadgesView extends View {

    private static final class Badge {
        final String emoji;
        final String label;
        final boolean earned;
        Badge(String e, String l, boolean b) { emoji = e; label = l; earned = b; }
    }

    private final List<Badge> badges = new ArrayList<>();
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BadgesView(Context c) { this(c, null); }
    public BadgesView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        bg.setColor(0xFFD8EFE3);
        dim.setColor(0x1A000000);
        txt.setTextAlign(Paint.Align.CENTER);
        lbl.setColor(0xFF1B4D3E);
        lbl.setTextAlign(Paint.Align.CENTER);
        lbl.setTextSize(26f);
    }

    public void refresh(Store store, int totalEntries, Context c) {
        int learned = store.countLearnedAll(c);
        int streak = store.streak();
        int quizzes = store.quizTotal();
        badges.clear();
        badges.add(new Badge("🌱", "اولین جمله", learned >= 1));
        badges.add(new Badge("📚", "۱۰۰ جمله", learned >= 100));
        badges.add(new Badge("🎓", "۵۰۰ جمله", learned >= 500));
        badges.add(new Badge("👑", "همهٔ جزوه", learned >= totalEntries && totalEntries > 0));
        badges.add(new Badge("🔥", "۳ روز پیوسته", streak >= 3));
        badges.add(new Badge("⚡", "۷ روز پیوسته", streak >= 7));
        badges.add(new Badge("🏅", "۱۰۰ روز پیوسته", streak >= 100));
        badges.add(new Badge("📝", "اولین آزمون", quizzes >= 1));
        badges.add(new Badge("🧠", "۵ آزمون", quizzes >= 5));
        // request layout since count is fixed; invalidate
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int perRow = 3;
        int rows = (int) Math.ceil(badges.size() / (float) perRow);
        int cw = MeasureSpec.getSize(w);
        int cell = Math.max((cw - 2 * 12) / perRow, 90);
        setMeasuredDimension(cw, Math.max(rows, 1) * cell);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (badges.isEmpty()) return;
        int perRow = 3;
        int cw = getWidth();
        int cell = (cw - 2 * 12) / perRow;
        txt.setTextSize(cell * 0.34f);
        lbl.setTextSize(cell * 0.12f);
        for (int i = 0; i < badges.size(); i++) {
            Badge b = badges.get(i);
            int col = i % perRow, row = i / perRow;
            float cx = 12 + col * cell + cell / 2f;
            float cy = row * cell + cell / 2f;
            float r = Math.min(cell, cell) * 0.36f;
            Paint p = b.earned ? bg : dim;
            canvas.drawCircle(cx, cy, r, p);
            canvas.drawText(b.earned ? b.emoji : "؟", cx, cy + txt.getTextSize() / 3f, txt);
            canvas.drawText(b.label, cx, cy + r + lbl.getTextSize(), lbl);
            if (!b.earned) {
                Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
                g.setColor(Color.argb(120, 255, 255, 255));
                canvas.drawCircle(cx, cy, r, g);
            }
        }
    }
}
