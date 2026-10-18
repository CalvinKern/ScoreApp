package com.seakernel.android.scoreapp

import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.utility.PlayerColors
import com.seakernel.android.scoreapp.utility.PlayerColors.GameColor
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerColorsTest {

    // Three seat colors, with different values in the other theme
    private val seatColors = listOf(1, 2, 3)
    private val otherThemeSeatColors = listOf(11, 12, 13)

    private fun colors(vararg playerColors: Int?) =
        PlayerColors.forGame(playerColors.map { Player(color = it) }, seatColors, otherThemeSeatColors)

    @Test
    fun playersWithoutAColorGetTheSeatColorsInOrder() {
        assertEquals(listOf(GameColor(1), GameColor(2), GameColor(3)), colors(null, null, null))
    }

    @Test
    fun seatColorsRepeatOnceTheyRunOut() {
        assertEquals(
            listOf(GameColor(1), GameColor(2), GameColor(3), GameColor(1, isRepeated = true)),
            colors(null, null, null, null),
        )
    }

    @Test
    fun aPickedSeatColorIsSkippedForEveryoneElse() {
        assertEquals(listOf(GameColor(1), GameColor(2), GameColor(3)), colors(null, 2, null))
        assertEquals(
            listOf(GameColor(1), GameColor(3), GameColor(1, isRepeated = true), GameColor(2)),
            colors(null, null, null, 2),
        )
    }

    @Test
    fun aSeatColorPickedInTheOtherThemeIsSkippedToo() {
        assertEquals(listOf(GameColor(12), GameColor(1), GameColor(3)), colors(12, null, null))
    }

    @Test
    fun aPickedColorThatIsNotASeatColorSkipsNothing() {
        assertEquals(listOf(GameColor(99), GameColor(1), GameColor(2)), colors(99, null, null))
    }

    @Test
    fun whenEverySeatColorIsPickedTheRestRepeatThem() {
        assertEquals(
            listOf(GameColor(1), GameColor(2), GameColor(3), GameColor(1, isRepeated = true), GameColor(2, isRepeated = true)),
            colors(1, 2, 3, null, null),
        )
    }
}
