package com.seakernel.android.scoreapp

import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.gamesetup.NewGameSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewGameSetupTest {

    private val alice = Player(id = 1, name = "Alice")
    private val bob = Player(id = 2, name = "Bob")
    private val carol = Player(id = 3, name = "Carol")

    private val settings = GameSettings(
        name = "Hearts",
        players = listOf(carol, alice, bob),
        initialDealerId = bob.id,
        hasDealer = false,
        showRounds = true,
        reversedScoring = true,
        maxScore = 100.0,
        maxRounds = 8,
        showRoundNotes = true,
        useCalculator = false,
    )

    /** What the settings are compared on, everything a new game keeps except when it was last played */
    private fun GameSettings.kept() = copy(lastPlayed = settings.lastPlayed)

    @Test
    fun keepsPlayersBySeat() {
        val setup = NewGameSetup.of(settings)

        assertEquals(listOf(3L, 1L, 2L), setup.playerIds)
    }

    @Test
    fun comesBackTheSame() {
        val setup = NewGameSetup.of(settings)

        assertEquals(settings, setup.toSettings(listOf(carol, alice, bob)).kept())
    }

    @Test
    fun comesBackWithPlayersAsTheyAreNow() {
        val renamed = alice.copy(name = "Alicia", color = 0xFF2E7D32.toInt())
        val restored = NewGameSetup.of(settings).toSettings(listOf(carol, renamed, bob))

        assertEquals(listOf(carol, renamed, bob), restored.players)
    }

    @Test
    fun aDeletedFirstDealerIsReplacedByTheFirstPlayer() {
        // Bob was deleted while the app was closed, so he isn't loaded again
        val restored = NewGameSetup.of(settings).toSettings(listOf(carol, alice))

        assertEquals(listOf(carol, alice), restored.players)
        assertEquals(carol.id, restored.initialDealerId)
    }

    @Test
    fun aGameWithoutPlayersHasNoFirstDealer() {
        val restored = NewGameSetup.of(settings.copy(players = emptyList(), initialDealerId = null)).toSettings(emptyList())

        assertNull(restored.initialDealerId)
        assertEquals("Hearts", restored.name)
    }

    @Test
    fun aNewGameHasNoId() {
        val restored = NewGameSetup.of(settings.copy(id = 7)).toSettings(listOf(carol, alice, bob))

        assertNull(restored.id)
    }
}
