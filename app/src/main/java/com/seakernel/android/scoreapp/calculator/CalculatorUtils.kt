package com.seakernel.android.scoreapp.calculator

import java.util.*

/**
 * Created by Calvin on 2020-03-14.
 * Copyright © 2020 SeaKernel. All rights reserved.
 */
object CalculatorUtils {
    const val DECIMAL = "."

    const val OPEN_PAREN = "("
    const val CLOSE_PAREN = ")"

    const val PLUS = "+"
    const val MINUS = "-"
    const val MULTIPLY = "x"
    const val DIVIDE = "/"

    // Doesn't quite work, splitting by regex will remove the matched patterns
    private val CALCULATOR_REGEX = Regex("(\\d*\\.?\\d+)(?![\\d*\\.?])|([()])|([+\\-x/])(?![+\\-x/])")
    private val VALIDATOR_REGEX =
        Regex("(${CALCULATOR_REGEX.pattern})+") // Doesn't handle double operators, nor right paren matching

    fun computeString(str: String): String? {
        val input = str.replace(" ", "")
        if (!VALIDATOR_REGEX.matches(input)) return null

        var sum = 0.0
        var holder = 0.0
        var sign = 1
        val stack = Stack<String>()

        for (match in CALCULATOR_REGEX.findAll(input)) {
            when {
                match.groupValues[1].isNotBlank() -> {
                    holder = match.groupValues[1].toDouble()
                }
                match.groupValues[2].isNotBlank() -> { /* TODO: Handle parenthesis */ }
                match.groupValues[3].isNotBlank() -> {
                    when (val operator = match.groupValues[3]) {
                        PLUS -> {
                            sum += holder * sign
                            sign = 1
                            holder = 0.0
                        }
                        MINUS -> {
                            sum += holder * sign
                            sign = -1
                            holder = 0.0
                        }
                        MULTIPLY, DIVIDE -> {
                            // 1 + 8 x 2
                            // sum = 1
                            // holder = 8
                            // match = x
//                            stack.push(sum.toString())
//                            stack.push((holder * sign).toString())
//                            stack.push(operator)
                            // TODO: figure out the rest of this...
                        }
                    }
                }
            }
        }

        val final = sum + (holder * sign)

        return if (final.toInt() - final == 0.0) {
            final.toInt().toString()
        } else {
            final.toString()
        }
    }
}