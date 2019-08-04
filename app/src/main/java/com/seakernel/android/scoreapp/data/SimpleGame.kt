package com.seakernel.android.scoreapp.data

import com.seakernel.android.scoreapp.database.GameEntity
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
// TODO: Rename to GameSettings
data class SimpleGame(
    val id: Long = 0,
    val name: String = "",
    val lastPlayed: ZonedDateTime = ZonedDateTime.now(),
    val players: List<Player> = listOf(),
    val initialDealerId: Long? = players.firstOrNull()?.id,
    val hasDealer: Boolean = true,
    val showRounds: Boolean = false,
    val reversedScoring: Boolean = false,
    val maxScore: Double? = null,
    val maxRounds: Int? = null
) {

    val lastPlayedAt: String = lastPlayed.format(UI_DATE_FORMATTER)

    fun toGameEntity() = GameEntity(
        id,
        name,
        ZonedDateTime.now().format(DATE_FORMATTER),
        hasDealer,
        showRounds,
        reversedScoring,
        maxScore,
        maxRounds
    )

    companion object {
        private const val DATE_FORMAT = "MMM dd, YYYY"
        val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME!!
        val UI_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT)!!
    }
}