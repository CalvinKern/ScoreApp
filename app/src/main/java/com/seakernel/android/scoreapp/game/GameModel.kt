package com.seakernel.android.scoreapp.game

import com.seakernel.android.scoreapp.data.FullGame
import com.seakernel.android.scoreapp.data.SimpleGame
import com.seakernel.android.scoreapp.data.Round
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class GameEvent
object RequestLoad : GameEvent()
data class RequestSaveRound(val round: Round) : GameEvent()
data class Loaded(val game: FullGame) : GameEvent()
data class RoundSaved(val round: Round) : GameEvent()

sealed class GameEffect
object FetchData : GameEffect()
data class SaveRound(val gameId: Long, val round: Round) : GameEffect()

data class GameModel(val game: SimpleGame = SimpleGame(), val rounds: List<Round> = emptyList()) {

    companion object {
        fun createDefault(): GameModel {
            return GameModel()
        }

        fun update(model: GameModel, event: GameEvent): Next<GameModel, GameEffect> {
            return when (event) {
                is Loaded -> Next.next(model.copy(game = event.game.simpleGame, rounds = event.game.rounds))
                is RequestLoad -> Next.dispatch(Effects.effects(FetchData))
                is RequestSaveRound -> Next.dispatch(Effects.effects(SaveRound(model.game.id, event.round)))
                is RoundSaved -> {
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
            }
        }
    }
}

