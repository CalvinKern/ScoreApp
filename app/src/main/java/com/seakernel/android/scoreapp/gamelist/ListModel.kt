package com.seakernel.android.scoreapp.gamelist

import com.seakernel.android.scoreapp.data.SimpleGame
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class ListEvent {
    object AddGameClicked : ListEvent()
    data class GameRowClicked(val gameId: Long) : ListEvent()
    data class GameRowLongPressed(val gameId: Long) : ListEvent()
    data class GameDeleteSuccessful(val gameId: Long) : ListEvent()
    object LoadData : ListEvent()
    data class Loaded(val games: List<SimpleGame>) : ListEvent()
}

sealed class ListEffect {
    object ShowCreateGameScreen : ListEffect()
    data class ShowGameScreen(val gameId: Long) : ListEffect()
    data class ShowGameRowDialog(val gameId: Long) : ListEffect()
    data class ShowDeleteSnackbar(val gameId: Long, val gameName: String?) : ListEffect()
    object FetchData : ListEffect()
}

data class ListModel(
    val gameList: List<SimpleGame> = listOf(),
    val isLoading: Boolean = false
) {

    companion object {
        fun createDefault(): ListModel {
            return ListModel()
        }

        fun update(model: ListModel, event: ListEvent): Next<ListModel, ListEffect> {
            return when (event) {
                is ListEvent.Loaded -> Next.next(model.copy(gameList = event.games, isLoading = false))
                is ListEvent.LoadData -> Next.next(model.copy(isLoading = true), Effects.effects(ListEffect.FetchData))
                is ListEvent.AddGameClicked -> Next.dispatch(Effects.effects(ListEffect.ShowCreateGameScreen))
                is ListEvent.GameRowClicked -> Next.dispatch(Effects.effects(ListEffect.ShowGameScreen(event.gameId)))
                is ListEvent.GameRowLongPressed -> Next.dispatch(Effects.effects(ListEffect.ShowGameRowDialog(event.gameId)))
                is ListEvent.GameDeleteSuccessful -> {
                    val name = model.gameList.find { it.id == event.gameId }?.name
                    val newModel = model.copy(gameList = model.gameList.filter { it.id != event.gameId })
                    Next.next(newModel, Effects.effects(ListEffect.ShowDeleteSnackbar(event.gameId, name)))
                }
            }
        }
    }
}

