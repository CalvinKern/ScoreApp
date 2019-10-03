package com.seakernel.android.scoreapp.playerselect
import com.seakernel.android.scoreapp.data.Player
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class PlayerEvent
object AddPlayerClicked : PlayerEvent()
object DoneSelectingPlayersClicked : PlayerEvent()
data class PlayerRowLongClicked(val playerId: Long) : PlayerEvent()
data class PlayerSelected(val playerId: Long, val selected: Boolean) : PlayerEvent()
data class PlayerDeleteClicked(val playerId: Long) : PlayerEvent()
data class PlayerNameChanged(val playerId: Long, val newName: String) : PlayerEvent()
data class PlayerDeleteSuccessful(val playerId: Long) : PlayerEvent()
data class PlayersLoaded(val players: List<Player>) : PlayerEvent()
data class PlayerSearchRequest(val playerName: String?) : PlayerEvent()
object RequestLoad : PlayerEvent()

sealed class PlayerEffect
data class DoneSelectingPlayers(val playerIds: List<Long>) : PlayerEffect()
data class ShowPlayerNameDialog(val playerId: Long?, val playerName: String) : PlayerEffect()
data class ShowDeleteDialog(val playerId: Long, val playerName: String?) : PlayerEffect()
data class ShowDeletePlayerSnackbar(val playerId: Long, val playerName: String?) : PlayerEffect()
object FetchData : PlayerEffect()

data class CreateModel(
    val gameId:Long = 0,
    val allPlayers: List<Player> = listOf(),
    val filteredPlayerList: List<Player> = listOf(),
    val gameName: String = "",
    val selectedPlayerList: List<Long> = listOf(),
    val isLoading: Boolean = false,
    val searchTerm: String = "") {

    fun player(playerId: Long): Player? {
        return allPlayers.find { it.id == playerId }
    }

    fun playerName(playerId: Long): String? {
        return player(playerId)?.name
    }

    companion object {
        fun createDefault(selectedPlayers: List<Long>?): CreateModel {
            return CreateModel(
                selectedPlayerList = selectedPlayers ?: listOf()
            )
        }

        fun update(model: CreateModel, event: PlayerEvent): Next<CreateModel, PlayerEffect> {
            return when (event) {
                is DoneSelectingPlayersClicked -> Next.dispatch(Effects.effects(DoneSelectingPlayers(model.selectedPlayerList)))
                is RequestLoad -> Next.next(model.copy(isLoading = true), Effects.effects(
                    FetchData
                ))
                is PlayersLoaded -> Next.next(model.copy(isLoading = false, allPlayers = event.players, filteredPlayerList = event.players))
                is AddPlayerClicked -> Next.dispatch(Effects.effects(
                    ShowPlayerNameDialog(
                        null,
                        ""
                    )
                ))
                is PlayerRowLongClicked -> {
                    val name = model.playerName(event.playerId)
                    Next.dispatch(Effects.effects(
                        ShowPlayerNameDialog(
                            event.playerId,
                            name ?: ""
                        )
                    ))
                }
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
                    Next.dispatch(Effects.effects(
                        ShowDeleteDialog(
                            event.playerId,
                            name
                        )
                    ))
                }
                is PlayerNameChanged -> {
                    val list = model.allPlayers.toMutableList()
                    val selected = model.selectedPlayerList.toMutableList()
                    val index = list.indexOfFirst { it.id == event.playerId }

                    val insertIndex = list.indexOfFirst { player -> player.name > event.newName }.let {
                        if (it >= 0) it else list.size
                    }
                    if (index >= 0) {
                        val oldPlayer = list.removeAt(index)
                        list.add(insertIndex, oldPlayer.copy(name = event.newName))
                    } else {
                        list.add(insertIndex, Player(event.playerId, event.newName))
                        selected.add(event.playerId)
                    }

                    val filteredList = if (event.newName.contains(model.searchTerm)) {
                        model.filteredPlayerList.toMutableList().also {
                            it.add(it.indexOfFirst { player -> player.name > event.newName }.let { index ->
                                if (index >= 0) index else it.size
                            }, Player(event.playerId, event.newName))
                        }
                    } else {
                        model.filteredPlayerList
                    }
                    Next.next(model.copy(allPlayers = list, selectedPlayerList = selected, filteredPlayerList = filteredList))
                }
                is PlayerDeleteSuccessful -> {
                    model.player(event.playerId)?.let { player ->
                        val list = model.allPlayers.toMutableList().also { it.remove(player) }
                        Next.next<CreateModel, PlayerEffect>(
                            model.copy(
                                allPlayers = list,
                                filteredPlayerList = model.filteredPlayerList.toMutableList().also { it.remove(player) },
                                selectedPlayerList = model.selectedPlayerList.toMutableList().also { it.remove(player.id) }
                            ), Effects.effects(
                            ShowDeletePlayerSnackbar(
                                event.playerId,
                                player.name
                            )
                        ))
                    } ?: Next.noChange()
                }
                is PlayerSearchRequest -> {
                    Next.next<CreateModel, PlayerEffect>(model.copy(
                        searchTerm = event.playerName ?: "",
                        filteredPlayerList = model.allPlayers.toMutableList().let { list ->
                        list.filter { player -> player.name.contains(event.playerName ?: "", ignoreCase = true) }
                    }))
                }
            }
        }
    }
}

