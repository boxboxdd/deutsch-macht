package com.michel.deutschmacht.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.michel.deutschmacht.data.Lessons
import com.michel.deutschmacht.R
import com.michel.deutschmacht.data.Store
import kotlin.math.ceil
import kotlin.math.min

/** Badge shelf: earned badges show their emoji, locked ones are dimmed with a "?". */
class BadgesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Badge(val emoji: String, val label: String, val earned: Boolean)

    private val badges = ArrayList<Badge>()

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.badge_bg) }
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.badge_dim) }
    private val locked = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.overlay_locked) }
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.green_dark)
        textAlign = Paint.Align.CENTER
    }

    private val perRow = 3
    private val padding = 12

    fun refresh(store: Store, totalEntries: Int) {
        val learned = store.countLearnedAll(context)
        val streak = store.streak()
        val quizzes = store.quizTotal()
        badges.clear()
        badges.add(Badge("🌱", "اولین جمله", learned >= 1))
        badges.add(Badge("📚", "۱۰۰ جمله", learned >= 100))
        badges.add(Badge("🎓", "۵۰۰ جمله", learned >= 500))
        badges.add(Badge("👑", "همهٔ جزوه", learned >= totalEntries && totalEntries > 0))
        badges.add(Badge("🔥", "۳ روز پیوسته", streak >= 3))
        badges.add(Badge("⚡", "۷ روز پیوسته", streak >= 7))
        badges.add(Badge("🏅", "۱۰۰ روز پیوسته", streak >= 100))
        badges.add(Badge("📝", "اولین آزمون", quizzes >= 1))
        badges.add(Badge("🧠", "۵ آزمون", quizzes >= 5))
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cw = MeasureSpec.getSize(widthMeasureSpec)
        val rows = ceil(badges.size / perRow.toFloat()).toInt()
        val cell = maxOf((cw - 2 * padding) / perRow, 90)
        setMeasuredDimension(cw, maxOf(rows, 1) * cell)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (badges.isEmpty()) return
        val cw = width
        val cell = (cw - 2 * padding) / perRow
        glyph.textSize = cell * 0.34f
        label.textSize = cell * 0.12f
        for (i in badges.indices) {
            val b = badges[i]
            val col = i % perRow
            val row = i / perRow
            val cx = padding + col * cell + cell / 2f
            val cy = row * cell + cell / 2f
            val r = min(cell, cell) * 0.36f
            canvas.drawCircle(cx, cy, r, if (b.earned) bg else dim)
            canvas.drawText(if (b.earned) b.emoji else "؟", cx, cy + glyph.textSize / 3f, glyph)
            canvas.drawText(b.label, cx, cy + r + label.textSize, label)
            if (!b.earned) canvas.drawCircle(cx, cy, r, locked)
        }
    }
}
