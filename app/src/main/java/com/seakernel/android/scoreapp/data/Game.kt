package com.seakernel.android.scoreapp.data

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
data class Game(val id: Long = 0,
                val name: String = "",
                val lastPlayed: ZonedDateTime = ZonedDateTime.now(),
                val players: List<Player> = listOf()) {
    val lastPlayedAt: String = lastPlayed.format(DateTimeFormatter.ofPattern("MMM dd, YYYY"))
}