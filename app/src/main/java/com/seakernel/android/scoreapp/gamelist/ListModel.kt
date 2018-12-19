package com.seakernel.android.scoreapp.gamelist

import com.seakernel.android.scoreapp.data.Game
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class ListEvent
object AddGameClicked : ListEvent()
data class GameRowClicked(val gameId: Long) : ListEvent()
data class GameRowLongPressed(val gameId: Long) : ListEvent()
data class GameDeleteSuccessful(val gameId: Long) : ListEvent()
data class AddGameSuccessful(val game: Game) : ListEvent()
object LoadData : ListEvent()
data class Loaded(val games: List<Game>) : ListEvent()

sealed class ListEffect
object ShowCreateGameScreen : ListEffect()
data class ShowGameScreen(val gameId: Long) : ListEffect()
data class ShowGameRowDialog(val gameId: Long) : ListEffect()
data class ShowDeleteSnackbar(val gameId: Long, val gameName: String?) : ListEffect()
object FetchData : ListEffect()

data class ListModel(val gameList: List<Game> = listOf()) {

    companion object {
        fun createDefault(): ListModel {
            return ListModel()
        }

        fun update(model: ListModel, event: ListEvent): Next<ListModel, ListEffect> {
            return when (event) {
                is Loaded -> Next.next(model.copy(gameList = event.games))
                is LoadData -> Next.dispatch(Effects.effects(FetchData))
                is AddGameClicked -> Next.dispatch(Effects.effects(ShowCreateGameScreen))
                is GameRowClicked -> Next.dispatch(Effects.effects(ShowGameScreen(event.gameId)))
                is GameRowLongPressed -> Next.dispatch(Effects.effects(ShowGameRowDialog(event.gameId)))
                is AddGameSuccessful -> Next.next(model.copy(gameList = model.gameList.plus(event.game)))
                is GameDeleteSuccessful -> {
                    val name = model.gameList.find { it.id == event.gameId }?.name
                    val newModel = model.copy(gameList = model.gameList.filter { it.id != event.gameId })
                    Next.next(newModel, Effects.effects(ShowDeleteSnackbar(event.gameId, name)))
                }
            }
        }
    }
}

