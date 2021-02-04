package com.seakernel.android.scoreapp.data

import com.seakernel.android.scoreapp.database.entities.GameEntity
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

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
    val showRounds: Boolean = false,
    val reversedScoring: Boolean = false,
    val maxScore: Double? = null,
    val maxRounds: Int? = null,
    val showRoundNotes: Boolean = false,
    val useCalculator: Boolean = true,
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

    val lastPlayedAt: String = lastPlayed.format(UI_DATE_FORMATTER)

    fun toGameEntity() =
        GameEntity(
            uid = id ?: 0,
            name = name,
            date = ZonedDateTime.now().format(DATE_FORMATTER),
            hasDealer = hasDealer,
            showRounds = showRounds,
            reversedScoring = reversedScoring,
            maxScore = maxScore,
            maxRounds = maxRounds,
            showRoundNotes = showRoundNotes,
            useCalculator = useCalculator,
        )

    companion object {
        private const val DATE_FORMAT = "MMM dd, YYYY"
        val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME!!
        val UI_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT)!!
    }
}