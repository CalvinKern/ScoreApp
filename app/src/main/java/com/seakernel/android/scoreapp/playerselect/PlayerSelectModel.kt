package com.seakernel.android.scoreapp.playerselect

import com.seakernel.android.scoreapp.data.Player
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class PlayerEvent
data object AddPlayerClicked : PlayerEvent()
data object DoneSelectingPlayersClicked : PlayerEvent()
data class PlayerRowLongClicked(val playerId: Long) : PlayerEvent()
data class PlayerSelected(val playerId: Long, val selected: Boolean) : PlayerEvent()
data class PlayerDeleteClicked(val playerId: Long) : PlayerEvent()
data class PlayerDeleteUndo(val playerId: Long, val playerSelected: Boolean) : PlayerEvent()
data class PlayerSaved(val player: Player) : PlayerEvent()
data class PlayerDeleteSuccessful(val playerId: Long) : PlayerEvent()
data class PlayersLoaded(val players: List<Player>) : PlayerEvent()
data class PlayerSearchRequest(val playerName: String?) : PlayerEvent()
data object RequestLoad : PlayerEvent()

sealed class PlayerEffect
data object DoneSelectingPlayers : PlayerEffect()
/** @param seatedPlayers the game being set up's players in seat order, with [player] in theirs, for its default color */
data class ShowPlayerDialog(val player: Player, val seatedPlayers: List<Player>) : PlayerEffect()
data class ShowDeletePlayerSnackbar(
    val playerId: Long,
    val playerName: String?,
    val playerSelected: Boolean
) : PlayerEffect()

data class UndoDeletePlayer(val playerId: Long) : PlayerEffect()
data object FetchData : PlayerEffect()

data class CreateModel(
    val gameId: Long = 0,
    val allPlayers: List<Player> = listOf(),
    val filteredPlayerList: List<Player> = listOf(),
    val gameName: String = "",
    val selectedPlayerList: List<Long> = listOf(),
    val isLoading: Boolean = false,
    val searchTerm: String = ""
) {

    fun player(playerId: Long): Player? {
        return allPlayers.find { it.id == playerId }
    }

    fun playerName(playerId: Long): String? {
        return player(playerId)?.name
    }

    /** The picked players in seat order, with [player] in their seat, or in the next one when they aren't picked */
    fun seatedWith(player: Player): List<Player> {
        val seated = selectedPlayerList.mapNotNull { id -> if (id == player.id) player else player(id) }
        return if (player.id in selectedPlayerList) seated else seated + player
    }

    companion object {
        /** Adds the player before the first player whose name sorts after it */
        private fun List<Player>.insertSorted(player: Player): List<Player> {
            val index = indexOfFirst { Player.NAME_ORDER.compare(it, player) > 0 }.let { if (it >= 0) it else size }
            return toMutableList().apply { add(index, player) }
        }

        /** The players whose names contain the search term, the list shown while searching */
        private fun List<Player>.matching(searchTerm: String) = filter { it.name.contains(searchTerm, ignoreCase = true) }

        private fun showPlayerDialog(model: CreateModel, player: Player): Next<CreateModel, PlayerEffect> =
            Next.dispatch(Effects.effects(ShowPlayerDialog(player, model.seatedWith(player))))

        fun createDefault(selectedPlayers: List<Long>?): CreateModel {
            return CreateModel(
                selectedPlayerList = selectedPlayers ?: listOf()
            )
        }

        fun update(model: CreateModel, event: PlayerEvent): Next<CreateModel, PlayerEffect> {
            return when (event) {
                is DoneSelectingPlayersClicked -> Next.dispatch(Effects.effects(DoneSelectingPlayers))
                is RequestLoad -> Next.next(
                    model.copy(isLoading = true), Effects.effects(
                        FetchData
                    )
                )
                is PlayersLoaded -> Next.next(
                    model.copy(
                        isLoading = false,
                        allPlayers = event.players,
                        // Players load again after undoing a delete, while the search can still be open
                        filteredPlayerList = event.players.matching(model.searchTerm)
                    )
                )
                is AddPlayerClicked -> showPlayerDialog(model, Player())
                is PlayerRowLongClicked -> model.player(event.playerId)?.let { showPlayerDialog(model, it) } ?: Next.noChange()
                is PlayerSelected -> {
                    val selected = model.selectedPlayerList.toMutableList()
                    if (event.selected) {
                        selected.find { it == event.playerId } ?: selected.add(event.playerId)
                    } else {
                        selected.remove(event.playerId)
                    }

                    if (selected.size == model.selectedPlayerList.size) {
                        Next.noChange()
                    } else {
                        Next.next(model.copy(selectedPlayerList = selected))
                    }
                }
                is PlayerDeleteClicked -> {
                    val name = model.playerName(event.playerId)
                    Next.dispatch(
                        Effects.effects(
                            ShowDeletePlayerSnackbar(
                                event.playerId,
                                name,
                                model.selectedPlayerList.contains(event.playerId)
                            )
                        )
                    )
                }
                is PlayerDeleteUndo -> {
                    val nextModel = if (event.playerSelected) {
                        model.copy(selectedPlayerList = model.selectedPlayerList + event.playerId)
                    } else {
                        model
                    }

                    Next.next(nextModel, Effects.effects(UndoDeletePlayer(event.playerId)))
                }
                is PlayerSaved -> {
                    val player = event.player
                    val existingPlayer = model.player(player.id!!)
                    val selected = model.selectedPlayerList.toMutableList()
                    if (existingPlayer == null) selected.add(player.id) // New players are picked right away

                    // Move the player to where the new name sorts, the same order the players are loaded in
                    val list = model.allPlayers.filterNot { it.id == player.id }.insertSorted(player)
                    val filteredList = model.filteredPlayerList.filterNot { it.id == player.id }.let {
                        if (player.name.contains(model.searchTerm, ignoreCase = true)) it.insertSorted(player) else it
                    }
                    Next.next(
                        model.copy(
                            allPlayers = list,
                            selectedPlayerList = selected,
                            filteredPlayerList = filteredList
                        )
                    )
                }
                is PlayerDeleteSuccessful -> {
                    model.player(event.playerId)?.let { player ->
                        val list = model.allPlayers.toMutableList().also { it.remove(player) }
                        Next.next(
                            model.copy(
                                allPlayers = list,
                                filteredPlayerList = model.filteredPlayerList.toMutableList()
                                    .also { it.remove(player) },
                                selectedPlayerList = model.selectedPlayerList.toMutableList()
                                    .also { it.remove(player.id) }
                            )
                        )
                    } ?: Next.noChange()
                }
                is PlayerSearchRequest -> {
                    Next.next(
                        model.copy(
                            searchTerm = event.playerName ?: "",
                            filteredPlayerList = model.allPlayers.matching(event.playerName ?: ""))
                    )
                }
            }
        }
    }
}

