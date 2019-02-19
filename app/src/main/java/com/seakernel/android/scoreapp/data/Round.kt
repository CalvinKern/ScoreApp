package com.seakernel.android.scoreapp.data

/**
 * Created by Calvin on 2/18/19.
 * Copyright © 2019 SeaKernel. All rights reserved.
 */
data class Round(
    val id: Long = 0,
    val dealer: Player? = null,
    val number: Int = 0,
    val scores: List<Score> = emptyList()
)