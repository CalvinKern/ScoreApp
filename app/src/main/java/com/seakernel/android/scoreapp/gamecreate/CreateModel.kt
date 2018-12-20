package com.seakernel.android.scoreapp.gamecreate
import com.seakernel.android.scoreapp.data.Player
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class CreateEvent
object AddPlayerClicked : CreateEvent()
data class StartGameClicked(val gameId: Long) : CreateEvent()
data class PlayerRowClicked(val playerId: Long) : CreateEvent()
data class PlayerSelected(val playerId: Long, val selected: Boolean) : CreateEvent()
data class PlayerDeleteClicked(val playerId: Long) : CreateEvent()
data class PlayerNameChanged(val playerId: Long, val newName: String) : CreateEvent()
data class PlayerDeleteSuccessful(val playerId: Long) : CreateEvent()
data class PlayersLoaded(val players: List<Player>) : CreateEvent()

sealed class CreateEffect
data class ShowGameScreen(val gameId: Long) : CreateEffect()
data class ShowPlayerNameDialog(val playerId: Long, val playerName: String) : CreateEffect()
data class ShowDeleteDialog(val playerId: Long, val playerName: String?) : CreateEffect()
data class ShowDeletePlayerSnackbar(val playerId: Long, val playerName: String?) : CreateEffect()
object FetchData : CreateEffect()

data class CreateModel(val playerList: List<Player> = listOf(), val selectedPlayerList: List<Long> = listOf()) {

    fun player(playerId: Long): Player? {
        return playerList.find { it.id == playerId }
    }

    fun playerName(playerId: Long): String? {
        return player(playerId)?.name
    }

    companion object {
        fun createDefault(): CreateModel {
            return CreateModel()
        }

        fun update(model: CreateModel, event: CreateEvent): Next<CreateModel, CreateEffect> {
            return when (event) {
                is PlayersLoaded -> Next.next(model.copy(playerList = event.players))
                is AddPlayerClicked -> Next.dispatch(Effects.effects(ShowPlayerNameDialog(0, "")))
                is StartGameClicked -> Next.dispatch(Effects.effects(ShowGameScreen(event.gameId)))
                is PlayerRowClicked -> {
                    val name = model.playerName(event.playerId)
                    Next.dispatch(Effects.effects(ShowPlayerNameDialog(event.playerId, name ?: "")))
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
                    Next.dispatch(Effects.effects(ShowDeleteDialog(event.playerId, name)))
                }
                is PlayerNameChanged -> {
                    val list = model.playerList.toMutableList()
                    val selected = model.selectedPlayerList.toMutableList()
                    val index = list.indexOfFirst { it.id == event.playerId }

                    if (index >= 0) {
                        val oldPlayer = list[index]
                        list[index] = oldPlayer.copy(name = event.newName)
                    } else {
                        list.add(Player(event.playerId, event.newName))
                        selected.add(event.playerId)
                    }

                    Next.next(model.copy(playerList = list, selectedPlayerList = selected))
                }
                is PlayerDeleteSuccessful -> {
                    model.player(event.playerId)?.let { player ->
                        val list = model.playerList.toMutableList().also { it.remove(player) }
                        Next.next<CreateModel, CreateEffect>(model.copy(playerList = list), Effects.effects(ShowDeletePlayerSnackbar(event.playerId, player.name)))
                    } ?: Next.noChange()
                }
            }
        }
    }
}

