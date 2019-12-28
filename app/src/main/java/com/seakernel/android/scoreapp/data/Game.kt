package com.seakernel.android.scoreapp.data

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
data class Game(
    val settings: GameSettings = GameSettings(),
    val rounds: List<Round> = emptyList()
)