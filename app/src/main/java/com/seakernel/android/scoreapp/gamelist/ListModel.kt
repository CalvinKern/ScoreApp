package com.seakernel.android.scoreapp.gamelist

import com.seakernel.android.scoreapp.data.GameSettings
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class ListEvent {
    data object AddGameClicked : ListEvent()
    data class GameRowClicked(val gameId: Long) : ListEvent()
    data class GameRowLongPressed(val gameId: Long) : ListEvent()
    data class GameDeleteSuccessful(val gameId: Long) : ListEvent()
    data object LoadData : ListEvent()
    data class Loaded(val games: List<GameSettings>, val askUsageData: Boolean) : ListEvent()
    data class UsageDataAnswered(val allowed: Boolean) : ListEvent()
}

sealed class ListEffect {
    data object ShowCreateGameScreen : ListEffect()
    data class ShowGameScreen(val gameId: Long) : ListEffect()
    data class ShowGameSetupScreen(val gameId: Long) : ListEffect()
    data class ShowGameRowDialog(val gameId: Long) : ListEffect()
    data class ShowDeleteSnackbar(val gameId: Long, val gameName: String?) : ListEffect()
    data object FetchData : ListEffect()
    data class SaveUsageDataAnswer(val allowed: Boolean) : ListEffect()
}

data class ListModel(
    val gameList: List<GameSettings> = listOf(),
    val isLoading: Boolean = false,
    val askUsageData: Boolean = false, // The user hasn't answered whether usage data can be shared yet
) {

    /** Asks about usage data in the list only once a game has been made, so it isn't the first thing a user sees */
    val showUsageDataCard: Boolean
        get() = askUsageData && gameList.isNotEmpty()

    companion object {
        fun createDefault(): ListModel {
            return ListModel()
        }

        fun update(model: ListModel, event: ListEvent): Next<ListModel, ListEffect> {
            return when (event) {
                is ListEvent.Loaded -> Next.next(
                    model.copy(gameList = event.games, isLoading = false, askUsageData = event.askUsageData)
                )
                is ListEvent.UsageDataAnswered -> Next.next(
                    model.copy(askUsageData = false),
                    Effects.effects(ListEffect.SaveUsageDataAnswer(event.allowed))
                )
                is ListEvent.LoadData -> Next.next(model.copy(isLoading = true), Effects.effects(ListEffect.FetchData))
                is ListEvent.AddGameClicked -> Next.dispatch(Effects.effects(ListEffect.ShowCreateGameScreen))
                is ListEvent.GameRowClicked -> {
                    // A game whose players were all deleted can't be played, so set up its players first
                    val hasNoPlayers = model.gameList.find { it.id == event.gameId }?.players?.isEmpty() == true
                    val effect = if (hasNoPlayers) {
                        ListEffect.ShowGameSetupScreen(event.gameId)
                    } else {
                        ListEffect.ShowGameScreen(event.gameId)
                    }
                    Next.dispatch(Effects.effects(effect))
                }
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

