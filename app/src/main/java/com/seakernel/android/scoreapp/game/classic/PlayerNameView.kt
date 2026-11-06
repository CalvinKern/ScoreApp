package com.seakernel.android.scoreapp.game.classic

import android.content.Context
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.roundToInt

/**
 * A player's name over their column. Names too wide for it shrink, from the theme's size down to a minimum, instead of
 * breaking mid-word. A name that's still too wide at the smallest size breaks with hyphens ("Mar-cus") instead of losing
 * its end ("Mar…").
 *
 * The size is picked each time the name or its column's width changes, before it's drawn. The platform's autosizing
 * doesn't fit: a word broken onto the next line still counts as fitting, and with a height that wraps its text it only
 * ever shrinks, so a name stayed small after its column got wider again (like when the keypad beside it hides).
 */
class PlayerNameView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    private val largestSize = textSize.roundToInt()
    private val smallestSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MIN_TEXT_SP, resources.displayMetrics)
        .roundToInt().coerceAtMost(largestSize)
    private val measurePaint = TextPaint()

    // What the size was last picked for, so it's only picked again when one of them changes
    private var fittedName: String? = null
    private var fittedWidth = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - compoundPaddingLeft - compoundPaddingRight
        val name = text.toString()
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && width > 0 &&
            (width != fittedWidth || name != fittedName)
        ) {
            fittedWidth = width
            fittedName = name
            val size = fittingSize(name, width).toFloat()
            if (size != textSize) setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** The largest size where each word of the name fits on a line of its own, or the smallest size if none does */
    private fun fittingSize(name: String, width: Int): Int {
        val words = name.trim().split(WHITESPACE)
        val lines = words.size.coerceIn(1, maxLines.takeIf { it > 0 } ?: Int.MAX_VALUE)
        val fits = { size: Int ->
            measurePaint.set(paint)
            measurePaint.textSize = size.toFloat()
            // Words can share a line, but none can break onto the next one or need more lines than there are words
            words.all { measurePaint.measureText(it) <= width } &&
                StaticLayout.Builder.obtain(name, 0, name.length, measurePaint, width)
                    .setBreakStrategy(breakStrategy)
                    .setHyphenationFrequency(hyphenationFrequency)
                    .setIncludePad(includeFontPadding)
                    .build()
                    .lineCount <= lines
        }

        if (!fits(smallestSize)) return smallestSize
        var low = smallestSize // Fits
        var high = largestSize + 1 // Too big
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (fits(middle)) low = middle else high = middle
        }
        return low
    }

    private companion object {
        const val MIN_TEXT_SP = 11f
        val WHITESPACE = Regex("\\s+")
    }
}
