package com.michel.deutschmacht.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 * ViewGroup that wraps children onto the next line when they no longer fit horizontally.
 * Used by the sentence-scramble game so word chips lay out naturally instead of overflowing.
 */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val horizontalSpacing = 8f.dpToPx()
    private val verticalSpacing = 8f.dpToPx()

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthLimit = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var currentLineWidth = 0
        var currentLineHeight = 0
        var maxLineWidth = 0
        var totalHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = child.layoutParams as MarginLayoutParams
            val cw = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val ch = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (currentLineWidth == 0 || currentLineWidth + horizontalSpacing + cw <= widthLimit) {
                currentLineWidth += if (currentLineWidth == 0) cw else horizontalSpacing + cw
                currentLineHeight = maxOf(currentLineHeight, ch)
            } else {
                maxLineWidth = maxOf(maxLineWidth, currentLineWidth)
                totalHeight += currentLineHeight + verticalSpacing
                currentLineWidth = cw
                currentLineHeight = ch
            }
        }
        if (currentLineWidth > 0) {
            maxLineWidth = maxOf(maxLineWidth, currentLineWidth)
            totalHeight += currentLineHeight
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            else -> maxOf(totalHeight, suggestedMinimumHeight)
        }
        setMeasuredDimension(resolveSize(maxLineWidth, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val widthLimit = right - left
        var currentX = 0
        var currentY = 0
        var currentLineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val lp = child.layoutParams as MarginLayoutParams
            val cw = child.measuredWidth
            val ch = child.measuredHeight

            if (currentX != 0 && currentX + horizontalSpacing + cw + lp.leftMargin + lp.rightMargin > widthLimit) {
                currentY += currentLineHeight + verticalSpacing
                currentX = 0
                currentLineHeight = 0
            }
            val childLeft = currentX + lp.leftMargin
            val childTop = currentY + lp.topMargin
            child.layout(childLeft, childTop, childLeft + cw, childTop + ch)
            currentX += horizontalSpacing + cw + lp.leftMargin + lp.rightMargin
            currentLineHeight = maxOf(currentLineHeight, ch + lp.topMargin + lp.bottomMargin)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
}
