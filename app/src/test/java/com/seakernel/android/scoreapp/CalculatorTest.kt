package com.seakernel.android.scoreapp

import com.seakernel.android.scoreapp.calculator.CalculatorUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CalculatorTest {
    @Test
    fun singles() {
        assertEquals("1", CalculatorUtils.eval("1"))
        assertEquals("0", CalculatorUtils.eval("0"))
        assertEquals("20.3", CalculatorUtils.eval("20.30"))
    }

    @Test
    fun addition() {
        assertEquals("4", CalculatorUtils.eval("2 + 2"))
        assertEquals("2", CalculatorUtils.eval("1 + 1"))
        assertEquals("7.2", CalculatorUtils.eval("4 + 3.2"))
        assertEquals("33", CalculatorUtils.eval("20 + 13 + 0"))
        assertEquals("123", CalculatorUtils.eval("100 + 20 + 3"))
    }

    @Test
    fun subtraction() {
        assertEquals("4", CalculatorUtils.eval("8 - 4"))
        assertEquals("0", CalculatorUtils.eval("4 - 4"))
        assertEquals("1", CalculatorUtils.eval("4 - 2 - 1"))
        assertEquals("2", CalculatorUtils.eval("4 - 2 - 0"))
    }

    @Test
    fun multiplication() {
        assertEquals("6", CalculatorUtils.eval("2 x 3"))
        assertEquals("9", CalculatorUtils.eval("3 x 3"))
        assertEquals("16", CalculatorUtils.eval("4 x 4"))
//        assertEquals("16", CalculatorUtils.eval("4 (4)"))
    }

    @Test
    fun division() {
        assertEquals("3", CalculatorUtils.eval("6 / 2"))
    }

    @Test
    fun complexArithmetic() {
        assertEquals("8", CalculatorUtils.eval("7 - 8 + 9"))
        assertEquals("10", CalculatorUtils.eval("9 + 8 - 7"))
    }

    @Test
    fun complexMultiplication() {
        assertEquals("5", CalculatorUtils.eval("1 + 2 x 3 - 2"))
    }

    @Test
    fun decimals() {
        assertEquals("12.3", CalculatorUtils.eval("10.1 + 2.2"))
    }

    @Test
    fun errors() {
        assertEquals(null, CalculatorUtils.eval(""))
        assertEquals(null, CalculatorUtils.eval("."))
        assertEquals(null, CalculatorUtils.eval("/"))
        assertEquals(null, CalculatorUtils.eval("+"))
        assertEquals(null, CalculatorUtils.eval("-"))
        assertEquals(null, CalculatorUtils.eval("x"))
        assertEquals(null, CalculatorUtils.eval("1++"))
        assertEquals(null, CalculatorUtils.eval("2+-+1"))
        assertEquals(null, CalculatorUtils.eval("2++1"))
        assertEquals(null, CalculatorUtils.eval("1--"))
        assertEquals(null, CalculatorUtils.eval("2--1"))
        assertEquals(null, CalculatorUtils.eval("1xx"))
        assertEquals(null, CalculatorUtils.eval("xx1"))
        assertEquals(null, CalculatorUtils.eval("1//"))
        assertEquals(null, CalculatorUtils.eval("//1"))
        assertEquals(null, CalculatorUtils.eval("1..0"))
        assertEquals(null, CalculatorUtils.eval("1.2.3"))
    }
}
