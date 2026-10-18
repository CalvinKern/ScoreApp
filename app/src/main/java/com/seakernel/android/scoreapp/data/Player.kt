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
    val archived: Boolean = false,
    val color: Int? = null, // An ARGB color, null to use the color of their seat in each game
) : Parcelable {
    companion object {
        /** Alphabetical, ignoring case, so "alice" isn't listed after "Zed" */
        @Suppress("SpellCheckingInspection")
        val NAME_ORDER: Comparator<Player> = compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
