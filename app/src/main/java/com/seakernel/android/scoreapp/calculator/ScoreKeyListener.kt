package com.seakernel.android.scoreapp.calculator

import android.text.InputType
import android.text.method.NumberKeyListener
import com.seakernel.android.scoreapp.utility.ScoreFormat

/**
 * Accepts signed decimal numbers typed with either a '.' or the user's decimal separator.
 *
 * The platform listener for `numberDecimal` only accepts '.', so a German "2,5" silently became 25.
 */
class ScoreKeyListener : NumberKeyListener() {
    private val acceptedChars = "0123456789-${CalculatorUtils.DECIMAL}${ScoreFormat.decimalSeparator()}"
        .toSet()
        .toCharArray()

    override fun getAcceptedChars(): CharArray = acceptedChars

    override fun getInputType(): Int =
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL
}
