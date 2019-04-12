package com.seakernel.android.scoreapp.data

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
data class SimpleGame(
    val id: Long = 0,
    val name: String = "",
    val lastPlayed: ZonedDateTime = ZonedDateTime.now(),
    val players: List<Player> = listOf()) {
    val lastPlayedAt: String = lastPlayed.format(UI_DATE_FORMATTER)

    companion object {
        private const val DATE_FORMAT = "MMM dd, YYYY"
        val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME!!
        val UI_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT)!!
    }
}