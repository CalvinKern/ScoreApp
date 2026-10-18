package com.seakernel.android.scoreapp.utility

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.utility.PlayerColors.forGame

/**
 * A player can pick their own color for every game. Everyone else gets a color by their seat in the game, so each
 * keeps it while the game goes on: the seat colors in order, skipping any a player in the game picked, so no two
 * players share a color until the seat colors run out. Larger games repeat them.
 */
object PlayerColors {
    private val seatColorIds = intArrayOf(
        R.color.graphGreen,
        R.color.graphCyan,
        R.color.graphPurple,
        R.color.graphOrange,
        R.color.graphBlue,
        R.color.graphRed,
        R.color.graphBrown,
        R.color.graphYellow,
        R.color.graphGray,
        R.color.graphPink,
    )

    /** @param isRepeated whether another player in the game has this color, so their line is dashed to tell them apart */
    data class GameColor(val color: Int, val isRepeated: Boolean = false)

    /** The colors a seat get, in order */
    fun seatColors(context: Context) = seatColorIds.map { ContextCompat.getColor(context, it) }

    /** Each player's color in a game, for [players] in seat order */
    fun forGame(context: Context, players: List<Player>) =
        forGame(players, seatColors(context), seatColors(inOtherTheme(context)))

    /** The color [players][seat] gets from their seat, as if they hadn't picked their own */
    fun forSeat(context: Context, players: List<Player>, seat: Int): Int {
        val withoutTheirColor = players.mapIndexed { index, player ->
            if (index == seat) player.copy(color = null) else player
        }
        return forGame(context, withoutTheirColor)[seat].color
    }

    /**
     * [forGame] with the seat colors in this theme and the other one. Seat colors are lighter in dark mode, and a
     * color picked from a swatch in either theme still takes that seat color out of the game.
     */
    internal fun forGame(players: List<Player>, seatColors: List<Int>, otherThemeSeatColors: List<Int>): List<GameColor> {
        val picked = players.mapNotNull { it.color }.toSet()
        val free = seatColors.filterIndexed { index, color -> color !in picked && otherThemeSeatColors[index] !in picked }
        val pool = free.ifEmpty { seatColors } // Every seat color was picked, so they all repeat
        val taken = picked.toMutableSet()
        var next = 0
        return players.map { player ->
            player.color?.let { GameColor(it) } ?: pool[next++ % pool.size].let { color ->
                GameColor(color, isRepeated = free.isEmpty() || !taken.add(color))
            }
        }
    }

    private fun inOtherTheme(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        val isNight = config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val otherNightMode = if (isNight) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or otherNightMode
        return context.createConfigurationContext(config)
    }

    /** The color as 6 hex digits (RRGGBB), without a # */
    fun toHex(color: Int) = "%06X".format(color and 0xFFFFFF)

    /** An opaque color from 6 hex digits (RRGGBB), with or without a #, or null if it isn't one */
    fun fromHex(hex: String): Int? =
        hex.trim().removePrefix("#").takeIf { HEX.matches(it) }?.toInt(16)?.or(0xFF000000.toInt())

    private val HEX = Regex("[0-9A-Fa-f]{6}")
}
