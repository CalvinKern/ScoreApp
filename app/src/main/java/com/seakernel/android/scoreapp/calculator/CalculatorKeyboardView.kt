package com.seakernel.android.scoreapp.calculator

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.calculator.CalculatorKeyboardView.Companion.KEYCODE_NEXT
import com.seakernel.android.scoreapp.databinding.ViewCalculatorKeyboardBinding
import com.seakernel.android.scoreapp.utility.ScoreFormat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

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

        // How long the keypad waits to hide once no score has focus, in case focus is moving to another score
        const val HIDE_DELAY_MS = 100L

        const val KEYCODE_EQUALS = KeyEvent.KEYCODE_EQUALS
        const val KEYCODE_NEXT = EditorInfo.IME_ACTION_NEXT
    }

    private var inputView: EditText? = null
    private var inputChangedListener: InputChangedListener? = null

    var isCalculatorEnabled = true
        set(value) {
            field = value

            // Immediately hide if not enabled
            if (!value) visibility = GONE
        }

    /** Called whenever the keypad shows or hides, e.g. to make room for it */
    var onShownChanged: ((shown: Boolean) -> Unit)? = null

    override fun setVisibility(visibility: Int) {
        val wasShown = isVisible
        super.setVisibility(visibility)
        if (isVisible != wasShown) onShownChanged?.invoke(isVisible)
    }

    private var delayCheckJob: Job? = null
    private var calculatorFailed = false
    private var calculatorString = ""
    private var calculatorEditIndex = 0
        set(value) {
            field = if (value < 0) 0 else value
        }
    private var _binding: ViewCalculatorKeyboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        _binding = ViewCalculatorKeyboardBinding.inflate(inflater, this, true)

        setupView()
        setKeyboardListeners()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setInputChangedListener(
        calculator: String? = null,
        inputListener: InputChangedListener? = null,
    ) {
        inputChangedListener = inputListener

        // Reset state for new listener
        calculatorString = calculator ?: ""
        calculatorEditIndex = calculatorString.length
    }

    private var hideJob: Job? = null

    /**
     * @param inputText the calculator input for the current query
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun setInput(inputText: EditText?) {
        hideJob?.cancel()
        if (inputText == null) {
            // Delay hiding slightly to allow for focus transitions between scores (e.g. when adding a round)
            hideJob = GlobalScope.launch(Dispatchers.Main) {
                delay(HIDE_DELAY_MS.milliseconds)
                if (!isAttachedToWindow) return@launch

                inputView = null
                visibility = GONE
                setInputChangedListener()

                // Hide system keyboard if it's showing
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.hideSoftInputFromWindow(windowToken, 0)
            }
            return
        }

        if (!isCalculatorEnabled) {
            inputView = null
            visibility = GONE
            return setInputChangedListener()
        }

        visibility = VISIBLE

        // Hide system keyboard if it's showing (should be handled by showSoftInputOnFocus = false, but just in case)
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(inputText.windowToken, 0)

        calculatorEditIndex = calculatorString.length

        inputView = inputText
        inputText.setOnKeyListener(::onHardwareKey)

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
            inputListener = { input, failed ->
                // Return unless we still have a reference
                val editText = weakInputText.get() ?: return@setInputChangedListener

                delayCheckJob?.cancel()
                delayCheckJob = GlobalScope.launch(Dispatchers.IO) {
                    delay(DELAY_VALID_COMPUTATION_MESSAGE.milliseconds)
                    val weakEdit = weakInputText.get() ?: return@launch
                    post { // Need the main thread for editText
                        weakEdit.error =
                            if (calculatorFailed && input.isNotEmpty() && weakEdit.hasFocus())
                                resources.getString(R.string.incomplete)
                            else null // Always need to clear here in case it's a duplicate job finishing early
                    }
                }

                if (!failed) editText.error = null // Reset the error since they just typed
                editText.setText(input)
                // Set the selection to our edit index (or the length if editing the end)
                editText.setSelection(min(calculatorEditIndex, input.length))
            }
        )
    }

    private fun setupView() {
        alignmentMode = ALIGN_MARGINS

        // Show the decimal separator of the user's language, so what is typed matches how scores are shown
        binding.calculatorKeyDecimal.text = ScoreFormat.decimalSeparator().toString()
    }

    private fun setKeyboardListeners() {
        val listener = { button: View -> onButtonClicked((button as TextView).text.toString()) }

        binding.calculatorKeyOne.setOnClickListener(listener)
        binding.calculatorKeyTwo.setOnClickListener(listener)
        binding.calculatorKeyThree.setOnClickListener(listener)
        binding.calculatorKeyFour.setOnClickListener(listener)
        binding.calculatorKeyFive.setOnClickListener(listener)
        binding.calculatorKeySix.setOnClickListener(listener)
        binding.calculatorKeySeven.setOnClickListener(listener)
        binding.calculatorKeyEight.setOnClickListener(listener)
        binding.calculatorKeyNine.setOnClickListener(listener)
        binding.calculatorKeyZero.setOnClickListener(listener)
        binding.calculatorKeyPlus.setOnClickListener(listener)
        binding.calculatorKeyMinus.setOnClickListener(listener)
        binding.calculatorKeyMultiply.setOnClickListener(listener)
        binding.calculatorKeyDivide.setOnClickListener(listener)
        binding.calculatorKeyEquals.setOnClickListener(listener)
        binding.calculatorKeyDelete.setOnClickListener(listener)
        binding.calculatorKeyDecimal.setOnClickListener(listener)
        binding.calculatorKeyOpenParen.setOnClickListener(listener)
        binding.calculatorKeyCloseParen.setOnClickListener(listener)

        binding.calculatorKeyNext.setOnClickListener { onNextClicked() }
        binding.calculatorKeyPrev.setOnClickListener { onPrevClicked() }
    }

    /**
     * Lets a hardware keyboard type into the calculator the same as its keys: tab and shift+tab move like next and
     * previous. Keys the calculator doesn't have (e.g. arrows, enter, ctrl+v) are left to the score's text field.
     */
    private fun onHardwareKey(view: View, keyCode: Int, event: KeyEvent): Boolean {
        if (view !== inputView || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false

        val onPress: () -> Unit = when (keyCode) {
            KeyEvent.KEYCODE_TAB -> if (event.isShiftPressed) ::onPrevClicked else ::onNextClicked
            else -> calculatorKey(keyCode, event)?.let { key -> { onButtonClicked(key) } } ?: return false
        }

        // Only the press counts, the release goes to whichever score has focus by then
        if (event.action == KeyEvent.ACTION_DOWN) onPress()
        return true
    }

    /** The calculator key a hardware key stands for, e.g. "×" for '*' or 'x' */
    private fun calculatorKey(keyCode: Int, event: KeyEvent): String? {
        if (keyCode == KeyEvent.KEYCODE_DEL) return resources.getString(R.string.del)

        val decimalSeparator = ScoreFormat.decimalSeparator()
        return when (val char = event.getUnicodeChar(event.metaState).toChar()) {
            in '0'..'9' -> char.toString()
            CalculatorUtils.DECIMAL, decimalSeparator -> decimalSeparator.toString()
            CalculatorUtils.PLUS -> resources.getString(R.string.plus)
            CalculatorUtils.MINUS -> resources.getString(R.string.minus)
            '*', 'x', 'X', '×' -> resources.getString(R.string.multiply)
            CalculatorUtils.DIVIDE, '÷' -> resources.getString(R.string.divide)
            CalculatorUtils.OPEN_PAREN -> resources.getString(R.string.open_paren)
            CalculatorUtils.CLOSE_PAREN -> resources.getString(R.string.close_paren)
            '=' -> resources.getString(R.string.equals)
            else -> null
        }
    }

    /**
     * Picks up text changed outside the calculator (e.g. a forward delete, or a keyboard app typing for a hardware
     * keyboard), so the next key edits what's shown instead of putting back the calculator's older text.
     */
    private fun syncWithInput() {
        val text = inputView?.text?.toString() ?: return
        // A zero score starts the calculator empty (see setInput), which isn't a change
        if (text == calculatorString || (calculatorString.isEmpty() && text.toDoubleOrNull() == 0.0)) return
        calculatorString = text
    }

    private fun onButtonClicked(key: String) {
        syncWithInput()
        calculatorEditIndex = inputView?.selectionStart ?: calculatorEditIndex

        val failure = !appendToString(key)

        // Equals saves the score and clears focus, after which the cell shows the saved score instead
        if (inputView?.hasFocus() != true) return
        inputChangedListener?.invoke(calculatorString, failure)
    }

    private fun onNextClicked() {
        inputView?.onEditorAction(KEYCODE_NEXT)
    }

    private fun onPrevClicked() {
        if (inputView?.layoutDirection == LAYOUT_DIRECTION_LTR) {
            inputView?.focusSearch(FOCUS_LEFT)?.requestFocus()
        } else {
            inputView?.focusSearch(FOCUS_RIGHT)?.requestFocus()
        }
    }

    /**
     * Checks to see if the key pressed can be added successfully to the current string
     * @return true if the string has changed, false otherwise
     */
    private fun appendToString(key: String): Boolean {
        if (inputView == null) return false
        calculatorString = when (key) {
            resources.getString(R.string.del) -> {
                // Works like backspace: removes the selected text, or the character before the cursor
                calculatorEditIndex = calculatorEditIndex.coerceAtMost(calculatorString.length)
                val selectionEnd = (inputView?.selectionEnd ?: calculatorEditIndex)
                    .coerceIn(calculatorEditIndex, calculatorString.length)
                when {
                    selectionEnd > calculatorEditIndex -> calculatorString.removeRange(calculatorEditIndex, selectionEnd)
                    calculatorEditIndex == 0 -> calculatorString // Nothing before the cursor to delete
                    else -> {
                        calculatorEditIndex--
                        calculatorString.removeRange(calculatorEditIndex, calculatorEditIndex + 1)
                    }
                }
            }

            resources.getString(R.string.equals) -> {
                inputView?.onEditorAction(KEYCODE_EQUALS)
                computeString()?.let(CalculatorUtils::toDisplay) ?: calculatorString
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
            // A cell that lost focus (after equals) shows its saved score, including any mistake, instead
            inputView?.takeIf { it.hasFocus() }?.error =
                if (calculatorFailed && key == resources.getString(R.string.equals)) {
                    resources.getString(R.string.incomplete)
                } else {
                    null
                }
        }
    }

    private fun computeString(): String? {
        return CalculatorUtils.eval(calculatorString, context)
    }
}
