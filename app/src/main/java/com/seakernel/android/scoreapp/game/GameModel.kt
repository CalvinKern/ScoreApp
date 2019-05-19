package com.seakernel.android.scoreapp.game

import com.seakernel.android.scoreapp.data.FullGame
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.SimpleGame
import com.seakernel.android.scoreapp.repository.GameRepository
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class GameEvent {
    object RequestLoad : GameEvent()
    data class RequestSaveRound(val round: Round) : GameEvent()
    data class Loaded(val game: FullGame) : GameEvent()
    data class RoundSaved(val round: Round) : GameEvent()
    data class UpdateScore(val roundId: Long, val playerId: Long, val score: Int, val metadata: String) : GameEvent()
}

sealed class GameEffect {
    object FetchData : GameEffect()
    data class SaveRound(val gameId: Long, val round: Round) : GameEffect()
}

data class GameModel(val settings: SimpleGame = SimpleGame(), val rounds: List<Round> = emptyList()) {

    companion object {
        fun createDefault(): GameModel {
            return GameModel()
        }

        fun update(model: GameModel, event: GameEvent): Next<GameModel, GameEffect> {
            return when (event) {
                is GameEvent.Loaded -> Next.next(model.copy(settings = event.game.settings, rounds = event.game.rounds))
                is GameEvent.RequestLoad -> Next.dispatch(Effects.effects(GameEffect.FetchData))
                is GameEvent.RequestSaveRound -> Next.dispatch(Effects.effects(
                    GameEffect.SaveRound(
                        model.settings.id,
                        event.round
                    )
                ))
                is GameEvent.RoundSaved -> {
                    val rounds = model.rounds.toMutableList()
                    val index = rounds.indexOfFirst { it.id == event.round.id }
                    if (index >= 0) {
                        rounds.removeAt(index)
                        rounds.add(index, event.round)
                    } else {
                        rounds.add(event.round)
                    }
                    Next.next(model.copy(rounds = rounds))
                }
                is GameEvent.UpdateScore -> {
                    val rounds = model.rounds.toMutableList()
                    val round = rounds.first { it.id == event.roundId }
                    val scores = round.scores.map { if (it.player.id == event.playerId) it.copy(value = event.score, metadata = event.metadata) else it }

                    Next.dispatch(Effects.effects(GameEffect.SaveRound(model.settings.id, round.copy(scores = scores))))
                }
            }
        }
    }
}
