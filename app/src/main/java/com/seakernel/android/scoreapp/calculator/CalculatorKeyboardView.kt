package com.seakernel.android.scoreapp.calculator

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.seakernel.android.scoreapp.R
import kotlinx.android.synthetic.main.view_calculator_keyboard.view.*

typealias InputChangedListener = (input: String, failure: Boolean) -> Unit

/**
 * Created by Calvin on 2020-03-09.
 * Copyright © 2020 SeaKernel. All rights reserved.
 */
class CalculatorKeyboardView(context: Context, attrs: AttributeSet) : GridLayout(context, attrs) {

    private var inputChangedListener: InputChangedListener? = null

    private var calculatorString = ""
    private var calculatorEditIndex = 0
        set(value) {
            field = if (value < 0) 0 else value
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_calculator_keyboard, this, true)

        setupView()
        setKeyboardListeners()
    }

    fun setInputChangedListener(listener: InputChangedListener?, calculator: String? = null) {
        inputChangedListener = listener

        // Reset state for new listener
        calculatorString = calculator ?: ""
    }

    private fun setupView() {
        columnCount = 5
        alignmentMode = ALIGN_MARGINS
        setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
    }

    private fun setKeyboardListeners() {
        val listener = { button: View -> onButtonClicked((button as TextView).text.toString()) }

        calculatorKeyOne.setOnClickListener(listener)
        calculatorKeyTwo.setOnClickListener(listener)
        calculatorKeyThree.setOnClickListener(listener)
        calculatorKeyFour.setOnClickListener(listener)
        calculatorKeyFive.setOnClickListener(listener)
        calculatorKeySix.setOnClickListener(listener)
        calculatorKeySeven.setOnClickListener(listener)
        calculatorKeyEight.setOnClickListener(listener)
        calculatorKeyNine.setOnClickListener(listener)
        calculatorKeyZero.setOnClickListener(listener)
        calculatorKeyPlus.setOnClickListener(listener)
        calculatorKeyMinus.setOnClickListener(listener)
        calculatorKeyMultiply.setOnClickListener(listener)
        calculatorKeyDivide.setOnClickListener(listener)
        calculatorKeyEquals.setOnClickListener(listener)
        calculatorKeyDelete.setOnClickListener(listener)
        calculatorKeyDecimal.setOnClickListener(listener)
        calculatorKeyOpenParen.setOnClickListener(listener)
        calculatorKeyCloseParen.setOnClickListener(listener)
    }

    private fun onButtonClicked(key: String) {
        val failure = !appendToString(key)
        inputChangedListener?.invoke(calculatorString, failure)
    }

    /**
     * Checks to see if the key pressed can be added successfully to the current string
     * @return true if the string has changed, false otherwise
     */
    private fun appendToString(key: String): Boolean {
        calculatorString = when (key) {
            resources.getString(R.string.del) -> {
                if (calculatorString.isEmpty()) {
                    calculatorString
                } else {
                    calculatorEditIndex--
                    calculatorString.run {
                        removeRange(IntRange(calculatorEditIndex, calculatorEditIndex + 1))
                    }
                }
            }
            resources.getString(R.string.equals) -> {
                computeString() ?: calculatorString
            }
            else -> {
                calculatorEditIndex++ // Increment the index
                if (calculatorEditIndex > calculatorString.length) {
                    calculatorString.substring(0, calculatorEditIndex - 1)
                        .plus(key)
                        .plus(calculatorString.substring(calculatorEditIndex - 1))
                } else {
                    calculatorString.plus(key)
                }
            }
        }

        return computeString() != null
    }

    // Compute the string, replacing any localized values with static strings for ease of parsing
    private fun computeString(): String? {
        return CalculatorUtils.eval(
            calculatorString
                .replace(resources.getString(R.string.plus), CalculatorUtils.PLUS.toString())
                .replace(resources.getString(R.string.minus), CalculatorUtils.MINUS.toString())
                .replace(resources.getString(R.string.multiply), CalculatorUtils.MULTIPLY.toString())
                .replace(resources.getString(R.string.divide), CalculatorUtils.DIVIDE.toString())
                .replace(resources.getString(R.string.close_paren), CalculatorUtils.CLOSE_PAREN.toString())
                .replace(resources.getString(R.string.open_paren), CalculatorUtils.OPEN_PAREN.toString())
                .replace(resources.getString(R.string.decimal), CalculatorUtils.DECIMAL.toString())
                .replace(resources.getString(R.string.exponent), CalculatorUtils.EXPONENT.toString())
        )
    }
}
