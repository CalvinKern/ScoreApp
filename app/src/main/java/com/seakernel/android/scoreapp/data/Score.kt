package com.seakernel.android.scoreapp.data

/**
 * Created by Calvin on 12/30/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 *
 * @param input what was typed for the score (e.g. "10/3"), so it can be edited again. Stored in the calculator's
 * canonical form, null when nothing was typed yet.
 */
data class Score(val id: Long = 0,
                 val player: Player = Player(),
                 val value: Double = 0.0,
                 val metadata: String = "",
                 val input: String? = null)
