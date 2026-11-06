package com.seakernel.android.scoreapp.gamesetup

import android.os.Parcelable
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

/**
 * A new game being set up, kept with the screen's saved state so it comes back after Android closes the app in the
 * background. Players are kept by id, in seat order, since their names and colors can change before it comes back.
 */
@Parcelize
data class NewGameSetup(
    val name: String,
    val playerIds: List<Long>,
    val initialDealerId: Long?,
    val hasDealer: Boolean,
    val showRounds: Boolean,
    val reversedScoring: Boolean,
    val maxScore: Double?,
    val maxRounds: Int?,
    val showRoundNotes: Boolean,
    val useCalculator: Boolean,
) : Parcelable {

    /**
     * The settings again, with [players] as they are now: the ones still around, in the order they're given in (seat
     * order). A first dealer who's gone is replaced by the first player, the same as picking players does.
     */
    fun toSettings(players: List<Player>): GameSettings = GameSettings(
        name = name,
        lastPlayed = ZonedDateTime.now(),
        players = players,
        initialDealerId = initialDealerId?.takeIf { id -> players.any { it.id == id } } ?: players.firstOrNull()?.id,
        hasDealer = hasDealer,
        showRounds = showRounds,
        reversedScoring = reversedScoring,
        maxScore = maxScore,
        maxRounds = maxRounds,
        showRoundNotes = showRoundNotes,
        useCalculator = useCalculator,
    )

    companion object {
        fun of(settings: GameSettings) = NewGameSetup(
            name = settings.name,
            playerIds = settings.players.mapNotNull { it.id },
            initialDealerId = settings.initialDealerId,
            hasDealer = settings.hasDealer,
            showRounds = settings.showRounds,
            reversedScoring = settings.reversedScoring,
            maxScore = settings.maxScore,
            maxRounds = settings.maxRounds,
            showRoundNotes = settings.showRoundNotes,
            useCalculator = settings.useCalculator,
        )
    }
}
