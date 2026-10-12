package com.seakernel.android.scoreapp.utility

import android.content.Context
import androidx.core.content.ContextCompat
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player

/**
 * Players get colors by their seat in the game, so everyone in a game of up to 10 players has their own color and keeps
 * it while the game goes on. Larger games repeat the colors. A player can also pick their own color for every game.
 */
object PlayerColors {
    private val seatColorIds = intArrayOf(
        R.color.graphRed,
        R.color.graphBlue,
        R.color.graphGreen,
        R.color.graphOrange,
        R.color.graphPurple,
        R.color.graphCyan,
        R.color.graphBrown,
        R.color.graphYellow,
        R.color.graphGray,
        R.color.graphPink,
    )

    /** The colors seats get, in seat order */
    fun seatColors(context: Context) = seatColorIds.map { ContextCompat.getColor(context, it) }

    fun forSeat(context: Context, seat: Int) = ContextCompat.getColor(context, seatColorIds[seat % seatColorIds.size])

    fun forPlayer(context: Context, player: Player, seat: Int) = player.color ?: forSeat(context, seat)

    /** Whether the player shares their seat's color with an earlier seat, so their line is dashed to tell them apart */
    fun isRepeated(player: Player, seat: Int) = player.color == null && seat >= seatColorIds.size

    /** The color as 6 hex digits (RRGGBB), without a # */
    fun toHex(color: Int) = "%06X".format(color and 0xFFFFFF)

    /** An opaque color from 6 hex digits (RRGGBB), with or without a #, or null if it isn't one */
    fun fromHex(hex: String): Int? =
        hex.trim().removePrefix("#").takeIf { HEX.matches(it) }?.toInt(16)?.or(0xFF000000.toInt())

    private val HEX = Regex("[0-9A-Fa-f]{6}")
}
