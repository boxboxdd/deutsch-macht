package com.michel.deutschmacht.progress;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.michel.deutschmacht.data.Store;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Last-30-days activity grid: green square = active day, today outlined. */
public class CalendarView30 extends View {

    private Set<String> active = new HashSet<>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint empty = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CalendarView30(Context c) { this(c, null); }
    public CalendarView30(Context c, AttributeSet a) { super(c, a); init(c); }

    private void init(Context c) {
        empty.setColor(0x22000000);
        fill.setColor(0xFF2E7D5B);
        stroke.setColor(0xFFF2A94C);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(4f);
        text.setColor(0xFF1B4D3E);
        text.setTextSize(30f);
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void refresh(Store store) {
        active = new HashSet<>();
        for (String d : store.activeDays()) active.add(d);
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int cols = 10, rows = 3;
        int side = (Math.max(MeasureSpec.getSize(w), 300) - (cols - 1) * 8) / cols;
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), w), rows * side + (rows - 1) * 8);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int cols = 10;
        float side = (getWidth() - (cols - 1) * 8f) / cols;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -29);
        for (int i = 0; i < 30; i++) {
            int col = i % cols, row = i / cols;
            float x = col * (side + 8);
            float y = row * (side + 8);
            String d = f.format(cal.getTime());
            canvas.drawRoundRect(new RectF(x, y, x + side, y + side), 8, 8,
                    active.contains(d) ? fill : empty);
            if (i == 29) canvas.drawRoundRect(new RectF(x, y, x + side, y + side), 8, 8, stroke);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
}
