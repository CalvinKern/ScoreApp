package com.seakernel.android.scoreapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by Calvin on 12/17/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Parcelize
data class Player(
    val id: Long? = null,
    val name: String = "",
    val archived: Boolean = false
) : Parcelable
