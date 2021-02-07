package com.seakernel.android.scoreapp.calculator

import android.content.Context
import com.seakernel.android.scoreapp.R
import kotlin.math.pow

/**
 * Created by Calvin on 2020-03-14.
 * Copyright © 2020 SeaKernel. All rights reserved.
 */
object CalculatorUtils {
    const val DECIMAL = '.'

    const val OPEN_PAREN = '('
    const val CLOSE_PAREN = ')'

    const val PLUS = '+'
    const val MINUS = '-'
    const val MULTIPLY = 'x'
    const val DIVIDE = '/'
    const val EXPONENT = '^'

    // Doesn't quite work, splitting by regex will remove the matched patterns
    private val CALCULATOR_REGEX = Regex("(\\d*\\.?\\d+)(?![\\d*\\.?])|([()])|([+\\-x/])(?![+\\-x/])|\\s")
    private val VALIDATOR_REGEX =
        Regex("(${CALCULATOR_REGEX.pattern})+") // Doesn't handle double operators, nor right paren matching

    // Compute the string, replacing any localized values with static strings for ease of parsing
    private fun sanitize(input: String, context: Context): String = with(context) {
        input.replace(resources.getString(R.string.plus), PLUS.toString())
            .replace(resources.getString(R.string.minus), MINUS.toString())
            .replace(resources.getString(R.string.multiply), MULTIPLY.toString())
            .replace(resources.getString(R.string.divide), DIVIDE.toString())
            .replace(resources.getString(R.string.close_paren), CLOSE_PAREN.toString())
            .replace(resources.getString(R.string.open_paren), OPEN_PAREN.toString())
            .replace(resources.getString(R.string.decimal), DECIMAL.toString())
    }

    // Answer snagged from this post: https://stackoverflow.com/a/26227947/4472135
    fun eval(input: String, context: Context? = null): String? {
        val str = if (context != null) sanitize(input, context) else input
        if (!VALIDATOR_REGEX.matches(str)) return null

        val output = object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < str.length) str[pos].toInt() else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.toInt()) nextChar() // Eat whitespace
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double? {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) {
                    println("Unexpected: ${ch.toChar()}")
                    return null
                }
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor
            fun parseExpression(): Double? {
                var x = parseTerm() ?: return null
                while (true) {
                    when {
                        eat(PLUS.toInt()) -> x += parseTerm() ?: return null // addition
                        eat(MINUS.toInt()) -> x -= parseTerm() ?: return null // subtraction
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double? {
                var x = parseFactor() ?: return null
                while (true) {
                    when {
                        eat(MULTIPLY.toInt()) -> x *= parseFactor() ?: return null // multiplication
                        eat(DIVIDE.toInt()) -> x /= parseFactor() ?: return null // division
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double? {
                if (eat(PLUS.toInt())) return parseFactor() // unary plus
                if (eat(MINUS.toInt())) return parseFactor()?.times(-1) // unary minus
                var x: Double
                val startPos = pos
                if (eat(OPEN_PAREN.toInt())) { // parentheses
                    x = parseExpression() ?: return null
                    eat(CLOSE_PAREN.toInt())
                } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == DECIMAL.toInt()) { // numbers
                    while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == DECIMAL.toInt()) nextChar()
                    x = str.substring(startPos, pos).toDoubleOrNull() ?: return null
//                } else if (ch >= 'a'.toInt() && ch <= 'z'.toInt()) { // functions
//                    while (ch >= 'a'.toInt() && ch <= 'z'.toInt()) nextChar()
//                    val func = str.substring(startPos, pos)
//                    x = parseFactor() ?: return null
//                    x = when (func) {
//                            "sqrt" -> sqrt(x)
//                            "sin" -> sin(Math.toRadians(x))
//                            "cos" -> cos(Math.toRadians(x))
//                            "tan" -> tan(Math.toRadians(x))
//                            else -> {
//                                println("Unknown function: $func")
//                                return null
//                            }
//                        }
                } else {
                    println("Unexpected: ${ch.toChar()} ($ch)")
                    return null
                }
                if (eat(EXPONENT.toInt())) x = x.pow(parseFactor() ?: return null) // exponentiation
                return x
            }
        }.parse() ?: return null

        return if (output.toInt() - output == 0.0) {
            output.toInt().toString()
        } else {
            output.toString()
        }
    }
}