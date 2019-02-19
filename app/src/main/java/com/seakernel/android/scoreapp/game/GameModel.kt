package com.seakernel.android.scoreapp.game

import com.seakernel.android.scoreapp.data.FullGame
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.Round
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class GameEvent
object RequestLoad : GameEvent()
data class Loaded(val game: FullGame) : GameEvent()

sealed class GameEffect
object FetchData : GameEffect()

data class GameModel(val game: Game = Game(), val rounds: List<Round> = emptyList()) {

    companion object {
        fun createDefault(): GameModel {
            return GameModel()
        }

        fun update(model: GameModel, event: GameEvent): Next<GameModel, GameEffect> {
            return when (event) {
                is Loaded -> Next.next(model.copy(game = event.game.game, rounds = event.game.rounds))
                is RequestLoad -> Next.dispatch(Effects.effects(FetchData))
            }
        }
    }
}

