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
        assertEquals("1", CalculatorUtils.computeString("1"))
        assertEquals("0", CalculatorUtils.computeString("0"))
        assertEquals("0", CalculatorUtils.computeString("/"))
        assertEquals("0", CalculatorUtils.computeString("+"))
        assertEquals("0", CalculatorUtils.computeString("-"))
        assertEquals("0", CalculatorUtils.computeString("x"))
    }

    @Test
    fun addition() {
        assertEquals("4", CalculatorUtils.computeString("2 + 2"))
        assertEquals("2", CalculatorUtils.computeString("1 + 1"))
        assertEquals("7", CalculatorUtils.computeString("4 + 3.2"))
        assertEquals("33", CalculatorUtils.computeString("20 + 13 + 0"))
        assertEquals("123", CalculatorUtils.computeString("100 + 20 + 3"))
    }

    @Test
    fun subtraction() {
        assertEquals("4", CalculatorUtils.computeString("8 - 4"))
        assertEquals("0", CalculatorUtils.computeString("4 - 4"))
        assertEquals("1", CalculatorUtils.computeString("4 - 2 - 1"))
        assertEquals("2", CalculatorUtils.computeString("4 - 2 - 0"))
    }

    @Test
    fun multiplication() {
        assertEquals("6", CalculatorUtils.computeString("2 * 3"))
    }

    @Test
    fun division() {
        assertEquals("3", CalculatorUtils.computeString("6 / 2"))
    }

    @Test
    fun complexArithmetic() {
        assertEquals("8", CalculatorUtils.computeString("7 - 8 + 9"))
        assertEquals("10", CalculatorUtils.computeString("9 + 8 - 7"))
    }

    @Test
    fun complexMultiplication() {
        assertEquals("5", CalculatorUtils.computeString("1 + 2 * 3 - 2"))
    }

    @Test
    fun decimals() {
        assertEquals("12.3", CalculatorUtils.computeString("10.1 + 2.2"))
    }

    @Test
    fun errors() {
        assertEquals(null, CalculatorUtils.computeString(""))
        assertEquals(null, CalculatorUtils.computeString("."))
        assertEquals(null, CalculatorUtils.computeString("1++"))
        assertEquals(null, CalculatorUtils.computeString("++1"))
        assertEquals(null, CalculatorUtils.computeString("1--"))
        assertEquals(null, CalculatorUtils.computeString("--1"))
        assertEquals(null, CalculatorUtils.computeString("1xx"))
        assertEquals(null, CalculatorUtils.computeString("xx1"))
        assertEquals(null, CalculatorUtils.computeString("1//"))
        assertEquals(null, CalculatorUtils.computeString("//1"))
        assertEquals(null, CalculatorUtils.computeString("1..0"))
        assertEquals(null, CalculatorUtils.computeString("1.2.3"))
    }
}
