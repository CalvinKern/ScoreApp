package com.seakernel.android.scoreapp.data

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
// TODO: Rename to Game
data class FullGame(
    val settings: SimpleGame = SimpleGame(),
    val rounds: List<Round> = emptyList()
)