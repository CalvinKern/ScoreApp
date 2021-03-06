package com.seakernel.android.scoreapp.calculator

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.seakernel.android.scoreapp.R
import kotlinx.android.synthetic.main.view_calculator_keyboard.view.*
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.math.min

typealias InputChangedListener = (input: String, failure: Boolean) -> Unit

/**
 * When the system is allowed to handle the editor action [KEYCODE_NEXT], it will propagate and move focus to the next
 * view.
 *
 * Created by Calvin on 2020-03-09.
 * Copyright © 2020 SeaKernel. All rights reserved.
 */
class CalculatorKeyboardView(context: Context, attrs: AttributeSet) : GridLayout(context, attrs) {

    /** See [CalculatorKeyboardView] documentation for behavior */
    companion object {
        // Delay just long enough that they stopped typing so it's not too intrusive
        const val DELAY_VALID_COMPUTATION_MESSAGE = 1150L

        const val KEYCODE_EQUALS = KeyEvent.KEYCODE_EQUALS
        const val KEYCODE_NEXT = EditorInfo.IME_ACTION_NEXT
    }

    private var inputView: EditText? = null
    private var inputChangedListener: InputChangedListener? = null

    private var delayCheckJob: Job? = null
    private var calculatorFailed = false
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

    fun setInputChangedListener(
        calculator: String? = null,
        inputListener: InputChangedListener? = null,
    ) {
        inputChangedListener = inputListener

        // Reset state for new listener
        calculatorString = calculator ?: ""
        calculatorEditIndex = calculatorString.length
    }

    /**
     * @param inputText the calculator input for the current query
     */
    fun setInput(inputText: EditText) {
        calculatorEditIndex = calculatorString.length

        inputView = inputText

        val inputString = inputText.text.toString().let {
            if (it.toDoubleOrNull() == 0.0) {
                "" // Replace 0 with an empty string because it's not important enough
            } else {
                it
            }
        }

        // Set the listener to update our input text, safely letting it be collected
        // TODO: MEMORY LEAK HERE!!? (should release the weak reference when this view is disposed, move to member variable)
        val weakInputText = WeakReference(inputText)
        setInputChangedListener(
            inputString, // Reset the string
            inputListener = { input, _ ->
                // Return unless we still have a reference
                val editText = weakInputText.get() ?: return@setInputChangedListener

                delayCheckJob?.cancel()
                delayCheckJob = GlobalScope.launch {
                    delay(DELAY_VALID_COMPUTATION_MESSAGE)
                    post { // Need the main thread for editText
                        editText.error =
                            if (calculatorFailed) resources.getString(R.string.incomplete)
                            else null // Always need to clear here in case it's a duplicate job finishing early
                    }
                }

                editText.error = null // Reset the error since they just typed
                editText.setText(input)
                // Set the selection to our edit index (or the length if editing the end)
                editText.setSelection(min(calculatorEditIndex, input.length))
            }
        )
    }

    private fun setupView() {
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

        calculatorKeyNext.setOnClickListener{ onNextClicked() }
        calculatorKeyPrev.setOnClickListener{ onPrevClicked() }
    }

    private fun onButtonClicked(key: String) {
        calculatorEditIndex = inputView?.selectionStart ?: calculatorEditIndex

        val failure = !appendToString(key)
        inputChangedListener?.invoke(calculatorString, failure)
    }

    private fun onNextClicked() {
        inputView?.onEditorAction(KEYCODE_NEXT)
    }

    private fun onPrevClicked() {
        if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            inputView?.focusSearch(View.FOCUS_LEFT)?.requestFocus()
        } else {
            inputView?.focusSearch(View.FOCUS_RIGHT)?.requestFocus()
        }
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
                    try {
                        calculatorString.run {
                            removeRange(calculatorEditIndex, calculatorEditIndex + 1)
                        }
                    } catch (e: Throwable) {
                        // Default to empty string if error removing anything?
                        // This has been observed in weird selection cases (shouldn't be able to select though)
                        ""
                    }
                }
            }
            resources.getString(R.string.equals) -> {
                inputView?.onEditorAction(KEYCODE_EQUALS)
                computeString() ?: calculatorString
            }
            else -> {
                calculatorEditIndex++ // Increment the index
                if (calculatorEditIndex - 1 < calculatorString.length) {
                    calculatorString.substring(0, calculatorEditIndex - 1)
                        .plus(key)
                        .plus(calculatorString.substring(calculatorEditIndex - 1))
                } else {
                    calculatorString.plus(key)
                }
            }
        }

        return (computeString() != null).also { validEquation ->
            calculatorFailed = !validEquation
        }
    }

    private fun computeString(): String? {
        return CalculatorUtils.eval(calculatorString, context)
    }
}
