package com.seakernel.android.scoreapp

import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.game.classic.GameEffect
import com.seakernel.android.scoreapp.game.classic.GameEvent
import com.seakernel.android.scoreapp.game.classic.GameModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModelTest {

    private val alice = Player(id = 1, name = "Alice")
    private val bob = Player(id = 2, name = "Bob")
    private val carol = Player(id = 3, name = "Carol")
    private val players = listOf(alice, bob, carol)

    private var nextScoreId = 1L

    /** Each round is a list of scores in player order */
    private fun loadGame(settings: GameSettings, vararg rounds: List<Double>): GameModel {
        val game = Game(
            settings = settings,
            rounds = rounds.mapIndexed { index, values ->
                Round(
                    id = index + 1L,
                    number = index,
                    scores = values.mapIndexed { playerIndex, value ->
                        Score(id = nextScoreId++, player = players[playerIndex], value = value)
                    }
                )
            }
        )
        return GameModel.update(GameModel(), GameEvent.Loaded(game)).modelUnsafe()
    }

    private fun settings(
        reversedScoring: Boolean = false,
        maxScore: Double? = null,
        maxRounds: Int? = null,
    ) = GameSettings(
        id = 1,
        name = "Test",
        players = players,
        reversedScoring = reversedScoring,
        maxScore = maxScore,
        maxRounds = maxRounds,
    )

    @Test
    fun totalsAreSummedPerPlayer() {
        val model = loadGame(settings(), listOf(1.0, 2.0, 3.0), listOf(10.0, 0.0, -5.0))

        assertEquals(mapOf(1L to 11.0, 2L to 2.0, 3L to -2.0), model.playerTotals)
    }

    @Test
    fun leadIsHighestScore() {
        val model = loadGame(settings(), listOf(5.0, 9.0, 1.0))

        assertEquals(listOf(bob.id), model.leadPlayerIds)
    }

    @Test
    fun leadIsLowestScoreWhenReversed() {
        val model = loadGame(settings(reversedScoring = true), listOf(5.0, 9.0, 1.0))

        assertEquals(listOf(carol.id), model.leadPlayerIds)
    }

    @Test
    fun tiedLeadersAreAllShown() {
        val model = loadGame(settings(), listOf(9.0, 9.0, 1.0))

        assertEquals(listOf(alice.id, bob.id), model.leadPlayerIds)
    }

    @Test
    fun noLeadWhenEveryoneIsTied() {
        val model = loadGame(settings(), listOf(4.0, 4.0, 4.0))

        assertTrue(model.leadPlayerIds.isEmpty())
    }

    @Test
    fun goalNotReachedWithoutLimits() {
        val model = loadGame(settings(), listOf(500.0, 0.0, 0.0))

        assertFalse(model.isGoalReached)
    }

    @Test
    fun goalReachedWhenAPlayerReachesMaxScore() {
        assertFalse(loadGame(settings(maxScore = 100.0), listOf(99.0, 0.0, 0.0)).isGoalReached)
        assertTrue(loadGame(settings(maxScore = 100.0), listOf(100.0, 0.0, 0.0)).isGoalReached)
    }

    @Test
    fun reversedGoalNotReachedAtTheStart() {
        // Everyone starts at 0, which used to count as being under the max score
        val model = loadGame(settings(reversedScoring = true, maxScore = 100.0), listOf(0.0, 0.0, 0.0))

        assertFalse(model.isGoalReached)
        assertFalse(model.hasReachedGoalScore())
    }

    @Test
    fun reversedGoalReachedWhenAPlayerReachesMaxScore() {
        val model = loadGame(
            settings(reversedScoring = true, maxScore = 100.0),
            listOf(60.0, 20.0, 10.0),
            listOf(45.0, 5.0, 10.0),
        )

        assertTrue(model.isGoalReached)
        // The player that reached the max score is losing, the lowest score is still the leader
        assertEquals(listOf(carol.id), model.leadPlayerIds)
    }

    @Test
    fun goalReachedAtMaxRounds() {
        val oneRound = listOf(listOf(0.0, 0.0, 0.0))
        val twoRounds = oneRound + listOf(listOf(0.0, 0.0, 0.0))

        assertFalse(loadGame(settings(maxRounds = 2), *oneRound.toTypedArray()).isGoalReached)
        assertTrue(loadGame(settings(maxRounds = 2), *twoRounds.toTypedArray()).isGoalReached)
    }

    @Test
    fun zeroLimitsAreIgnored() {
        assertFalse(loadGame(settings(maxRounds = 0), listOf(0.0, 0.0, 0.0)).isGoalReached)
        assertFalse(loadGame(settings(maxScore = 0.0), listOf(0.0, 0.0, 0.0)).isGoalReached)
    }

    @Test
    fun requestFinishGameShowsFinishDialog() {
        val model = loadGame(settings(maxRounds = 1), listOf(0.0, 0.0, 0.0))
        val next = GameModel.update(model, GameEvent.RequestFinishGame)

        assertEquals(setOf(GameEffect.ShowFinishGame), next.effects())
    }

    @Test
    fun requestFinishGameIgnoredBeforeLoading() {
        val next = GameModel.update(GameModel(), GameEvent.RequestFinishGame)

        assertFalse(next.hasModel())
        assertTrue(next.effects().isEmpty())
    }
}
