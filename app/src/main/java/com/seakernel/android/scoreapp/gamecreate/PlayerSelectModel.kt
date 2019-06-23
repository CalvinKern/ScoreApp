package com.seakernel.android.scoreapp.gamecreate
import com.seakernel.android.scoreapp.data.Player
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class PlayerEvent
object AddPlayerClicked : PlayerEvent()
//object StartGameClicked : PlayerEvent()
object DoneSelectingPlayersClicked : PlayerEvent()
data class PlayerRowLongClicked(val playerId: Long) : PlayerEvent()
data class PlayerSelected(val playerId: Long, val selected: Boolean) : PlayerEvent()
data class PlayerDeleteClicked(val playerId: Long) : PlayerEvent()
data class PlayerNameChanged(val playerId: Long, val newName: String) : PlayerEvent()
data class PlayerDeleteSuccessful(val playerId: Long) : PlayerEvent()
data class PlayersLoaded(val players: List<Player>) : PlayerEvent()
object RequestLoad : PlayerEvent()
//data class GameNameChanged(val gameName: String) : PlayerEvent()

sealed class PlayerEffect
//data class ShowGameScreen(val gameId: Long) : PlayerEffect()
//data class SaveGame(val gameId: Long, val playerIds: List<Long>) : PlayerEffect()
data class DoneSelectingPlayers(val playerIds: List<Long>) : PlayerEffect()
data class ShowPlayerNameDialog(val playerId: Long, val playerName: String) : PlayerEffect()
data class ShowDeleteDialog(val playerId: Long, val playerName: String?) : PlayerEffect()
data class ShowDeletePlayerSnackbar(val playerId: Long, val playerName: String?) : PlayerEffect()
object FetchData : PlayerEffect()

data class CreateModel(
    val gameId:Long = 0,
    val playerList: List<Player> = listOf(),
    val gameName: String = "",
    val selectedPlayerList: List<Long> = listOf(),
    val loading: Boolean = false) {

    fun player(playerId: Long): Player? {
        return playerList.find { it.id == playerId }
    }

    fun playerName(playerId: Long): String? {
        return player(playerId)?.name
    }

    companion object {
        fun createDefault(selectedPlayers: List<Long>?): CreateModel {
            return CreateModel(selectedPlayerList = selectedPlayers ?: listOf())
        }

        fun update(model: CreateModel, event: PlayerEvent): Next<CreateModel, PlayerEffect> {
            return when (event) {
//                is GameNameChanged -> Next.next(model.copy(gameName = event.gameName))
                is DoneSelectingPlayersClicked -> Next.dispatch(Effects.effects())
                is RequestLoad -> Next.next(model.copy(loading = true), Effects.effects(FetchData))
                is PlayersLoaded -> Next.next(model.copy(playerList = event.players))
                is AddPlayerClicked -> Next.dispatch(Effects.effects(ShowPlayerNameDialog(0, "")))
//                is StartGameClicked -> Next.dispatch(Effects.effects(SaveGame(model.gameId, model.selectedPlayerList)))
                is PlayerRowLongClicked -> {
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
                        Next.next<CreateModel, PlayerEffect>(model.copy(playerList = list), Effects.effects(ShowDeletePlayerSnackbar(event.playerId, player.name)))
                    } ?: Next.noChange()
                }
            }
        }
    }
}

