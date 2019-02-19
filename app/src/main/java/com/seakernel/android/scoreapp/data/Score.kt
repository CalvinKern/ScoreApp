package com.seakernel.android.scoreapp.data

/**
 * Created by Calvin on 12/30/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
data class Score(val id: Long = 0,
                 val player: Player = Player(),
                 val value: Int = 0,
                 val metadata: String = "")