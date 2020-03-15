package com.seakernel.android.scoreapp.calculator

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
//
//    fun computeString(str: String): String? {
//        val input = str.replace(" ", "")
//        if (!VALIDATOR_REGEX.matches(input)) return null
//
//        var sum = 0.0
//        var holder = 0.0
//        var sign = 1
//        val stack = Stack<String>()
//
//        for (match in CALCULATOR_REGEX.findAll(input)) {
//            when {
//                match.groupValues[1].isNotBlank() -> {
//                    holder = match.groupValues[1].toDouble()
//                }
//                match.groupValues[2].isNotBlank() -> { /* TODO: Handle parenthesis */ }
//                match.groupValues[3].isNotBlank() -> {
//                    when (val operator = match.groupValues[3]) {
//                        PLUS -> {
//                            sum += holder * sign
//                            sign = 1
//                            holder = 0.0
//                        }
//                        MINUS -> {
//                            sum += holder * sign
//                            sign = -1
//                            holder = 0.0
//                        }
//                        MULTIPLY, DIVIDE -> {
//                            // 1 + 8 x 2
//                            // sum = 1
//                            // holder = 8
//                            // match = x
//                            stack.push(sum.toString())
//                            stack.push((holder * sign).toString())
//                            stack.push(operator)
//                            // TODO: figure out the rest of this...
//                        }
//                    }
//                }
//            }
//        }
//
//        val final = sum + (holder * sign)
//
//        return if (final.toInt() - final == 0.0) {
//            final.toInt().toString()
//        } else {
//            final.toString()
//        }
//    }

    // Answer snagged from this post: https://stackoverflow.com/a/26227947/4472135
    fun eval(str: String): String? {
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