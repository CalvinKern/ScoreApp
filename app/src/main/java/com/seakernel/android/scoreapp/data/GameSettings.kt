package com.seakernel.android.scoreapp.data

import com.seakernel.android.scoreapp.database.entities.GameEntity
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
data class GameSettings(
    val id: Long? = null,
    val name: String = "",
    val lastPlayed: ZonedDateTime = ZonedDateTime.now(),
    val players: List<Player> = listOf(),
    val initialDealerId: Long? = players.firstOrNull()?.id,
    val hasDealer: Boolean = true,
    val showRounds: Boolean = false, // showRounds deprecated in favor of showRoundNotes
    val reversedScoring: Boolean = false,
    val maxScore: Double? = null,
    val maxRounds: Int? = null,
    val showRoundNotes: Boolean = false, // Only shows the notes icon, notes can always be opened from a player's name
    val useCalculator: Boolean = false,
    val roundCount: Int = 0, // Only loaded for the game list
) {

    constructor(settings: GameEntity, players: List<Player>) : this(
        id = settings.uid,
        name = settings.name,
        lastPlayed = ZonedDateTime.parse(settings.date),
        players = players,
        hasDealer = settings.hasDealer,
        showRounds = settings.showRounds,
        reversedScoring = settings.reversedScoring,
        maxScore = settings.maxScore,
        maxRounds = settings.maxRounds,
        showRoundNotes = settings.showRoundNotes,
        useCalculator = settings.useCalculator,
    )

    /** The last played date in the user's language and date order, created each time so language changes apply */
    val lastPlayedAt: String
        get() = lastPlayed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    fun toGameEntity() =
        GameEntity(
            uid = id ?: 0,
            name = name,
            date = lastPlayed.format(DATE_FORMATTER),
            hasDealer = hasDealer,
            showRounds = showRounds,
            reversedScoring = reversedScoring,
            maxScore = maxScore,
            maxRounds = maxRounds,
            showRoundNotes = showRoundNotes,
            useCalculator = useCalculator,
        )

    companion object {
        val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME!!
    }
}
