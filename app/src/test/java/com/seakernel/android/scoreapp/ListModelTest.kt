package com.seakernel.android.scoreapp

import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.gamelist.ListEffect
import com.seakernel.android.scoreapp.gamelist.ListEvent
import com.seakernel.android.scoreapp.gamelist.ListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListModelTest {

    private val game = GameSettings(id = 1, name = "Game Night")

    private fun loaded(games: List<GameSettings>, askUsageData: Boolean): ListModel =
        ListModel.update(ListModel.createDefault(), ListEvent.Loaded(games, askUsageData)).modelUnsafe()

    @Test
    fun usageDataCardWaitsForAGame() {
        assertFalse(loaded(emptyList(), askUsageData = true).showUsageDataCard)
        assertTrue(loaded(listOf(game), askUsageData = true).showUsageDataCard)
    }

    @Test
    fun usageDataCardIsNotShownOnceAnswered() {
        assertFalse(loaded(listOf(game), askUsageData = false).showUsageDataCard)
    }

    @Test
    fun answeringHidesTheCardAndSavesTheAnswer() {
        val next = ListModel.update(loaded(listOf(game), askUsageData = true), ListEvent.UsageDataAnswered(true))

        assertFalse(next.modelUnsafe().showUsageDataCard)
        assertEquals(setOf(ListEffect.SaveUsageDataAnswer(true)), next.effects())
    }

    @Test
    fun deletingTheLastGameHidesTheUnansweredCard() {
        val model = ListModel.update(loaded(listOf(game), askUsageData = true), ListEvent.GameDeleteSuccessful(1))
            .modelUnsafe()

        assertFalse(model.showUsageDataCard)
    }
}
