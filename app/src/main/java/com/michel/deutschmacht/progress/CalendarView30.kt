package com.michel.deutschmacht.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Store
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Last-30-days activity grid: filled square = active day, today outlined in orange. */
class CalendarView30 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val active = HashSet<String>()

    private val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.fill_active)
    }
    private val empty = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.fill_empty)
    }
    private val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.today_stroke)
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cols = 10
    private val rows = 3
    private val gap = 8f

    fun refresh(store: Store) {
        active.clear()
        active.addAll(store.activeDays().toList())
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val side = (maxOf(MeasureSpec.getSize(widthMeasureSpec), 300) - (cols - 1) * gap.toInt()) / cols
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            rows * side + (rows - 1) * gap.toInt()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = (width - (cols - 1) * gap) / cols
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -29)
        for (i in 0 until 30) {
            val col = i % cols
            val row = i / cols
            val x = col * (side + gap)
            val y = row * (side + gap)
            val rect = RectF(x, y, x + side, y + side)
            canvas.drawRoundRect(rect, 8f, 8f, if (active.contains(fmt.format(cal.time))) fill else empty)
            if (i == 29) canvas.drawRoundRect(rect, 8f, 8f, stroke)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}
