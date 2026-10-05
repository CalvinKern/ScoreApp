package com.seakernel.android.scoreapp.utility

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Formats and parses scores with the user's decimal separator (e.g. "2,5" in German), so what is shown
 * can always be read back. The formatters are created per call so a change of language applies right away.
 */
object ScoreFormat {
    private const val PATTERN = "#.##"

    /** The decimal separator of the user's language */
    fun decimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator

    fun format(value: Double): String = DecimalFormat(PATTERN).format(value)

    /** Parses a plain number written with either a '.' or the user's decimal separator */
    fun parse(text: String): Double? = text.trim().replace(decimalSeparator(), '.').toDoubleOrNull()
}
