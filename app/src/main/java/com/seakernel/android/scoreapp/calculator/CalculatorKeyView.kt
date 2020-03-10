package com.seakernel.android.scoreapp.calculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.utility.dp
import kotlin.math.max

/**
 * A custom key for a calculator keyboard.
 */
class CalculatorKeyView : View {

    private var _keyText: String? = null
    private var _keyColor: Int = Color.RED // TODO: use a default from R.color...
    private var _valueColor: Int = Color.RED
    private var _textSize: Float = 0f // TODO: use a default from R.dimen...
    private var _keyDrawable: Drawable? = null

    private var textPaint: TextPaint? = null
    private var textWidth: Float = 0f
    private var textHeight: Float = 0f

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    /**
     * The text drawn for the key's value.
     * If [keyDrawable] is also provided, this is used as the accessibility string.
     */
    var keyText: String?
        get() = _keyText
        set(value) {
            _keyText = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * The color for this key.
     */
    var keyColor: Int
        get() = _keyColor
        set(value) {
            _keyColor = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * The color for this key.
     */
    var valueColor: Int
        get() = _valueColor
        set(value) {
            _valueColor = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * In the example view, this dimension is the font size.
     */
    var textSize: Float
        get() = _textSize
        set(value) {
            _textSize = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * The drawable used to represent this key's action
     */
    var keyDrawable: Drawable?
        get() = _keyDrawable
        set(value) {
            _keyDrawable = value
            invalidateTextPaintAndMeasurements()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.CalculatorKeyView, defStyle, 0
        )

        _keyText = a.getString(R.styleable.CalculatorKeyView_keyText)
        _keyColor = a.getColor(R.styleable.CalculatorKeyView_keyColor, keyColor)
        _valueColor = a.getColor(R.styleable.CalculatorKeyView_valueColor, valueColor)
        _textSize = a.getDimension(R.styleable.CalculatorKeyView_textSize, textSize)

        if (a.hasValue(R.styleable.CalculatorKeyView_keyDrawable)) {
            keyDrawable = a.getDrawable(R.styleable.CalculatorKeyView_keyDrawable)
            keyDrawable?.callback = this
        }

        a.recycle()

        // Set up a default TextPaint object
        textPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = Paint.Align.CENTER
        }

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements()
    }

    private fun invalidateTextPaintAndMeasurements() {
        textPaint?.let {
            it.color = keyColor
            it.textSize = textSize
            textHeight = it.descent() + it.ascent()
            textWidth =
                if (keyDrawable == null && keyText != null) it.measureText(keyText) else 100.0f
        }

        // TODO: optional? (don't do from init?)
        invalidate()
        requestLayout()
    }

    override fun getSuggestedMinimumWidth() = 48.dp
    override fun getSuggestedMinimumHeight() = 48.dp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Try for a width based on our minimum
        val minw = max(paddingLeft + paddingRight + textWidth.toInt(), suggestedMinimumWidth)
        val w = resolveSizeAndState(minw, widthMeasureSpec, 0)

        val minh = max(
            paddingBottom + paddingTop + MeasureSpec.getSize(w) - textWidth.toInt(),
            suggestedMinimumHeight
        )
        val h = resolveSizeAndState(minh, heightMeasureSpec, 0)

        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        contentWidth = width - (paddingLeft + paddingRight)
        contentHeight = height - (paddingTop + paddingBottom)
        keyDrawable?.setBounds(
            paddingLeft, paddingTop,
            paddingLeft + contentWidth, paddingTop + contentHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the drawable if possible, otherwise draw the text if possible
        keyDrawable?.draw(canvas) ?: keyText?.let {
            // Draw the text.
            canvas.drawText(
                it,
                paddingLeft + contentWidth / 2.0f,
                paddingTop + (contentHeight - textHeight) / 2.0f,
                textPaint!!
            )
        }
    }
}
