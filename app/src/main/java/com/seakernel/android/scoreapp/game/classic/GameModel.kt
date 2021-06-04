package com.seakernel.android.scoreapp.game.classic

import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.data.GameSettings
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class GameEvent {
    object RequestLoad : GameEvent()
    object RequestCreateRound : GameEvent()
    data class RequestSaveRound(val round: Round) : GameEvent()
    data class Loaded(val game: Game) : GameEvent()
    data class RoundSaved(val round: Round) : GameEvent()
    data class ScoreSaved(val roundId: Long, val score: Score) : GameEvent()
    data class UpdateScore(val roundId: Long, val playerId: Long, val score: Double, val metadata: String) : GameEvent()
}

sealed class GameEffect {
    object FetchData : GameEffect()
    data class SaveRound(val gameId: Long, val round: Round) : GameEffect()
    data class SaveScore(val roundId: Long, val score: Score) : GameEffect()
}

data class GameModel(val settings: GameSettings = GameSettings(), val rounds: List<Round> = emptyList()) {

    companion object {
        fun createDefault(): GameModel {
            return GameModel()
        }

        fun update(model: GameModel, event: GameEvent): Next<GameModel, GameEffect> {
            return when (event) {
                is GameEvent.Loaded -> Next.next(model.copy(settings = event.game.settings, rounds = event.game.rounds))
                is GameEvent.RequestLoad -> Next.dispatch(Effects.effects(
                    GameEffect.FetchData
                ))
                is GameEvent.RequestSaveRound -> Next.dispatch(Effects.effects(
                    GameEffect.SaveRound(
                        model.settings.id!!,
                        event.round
                    )
                ))
                GameEvent.RequestCreateRound -> {
                    val lastRound = model.rounds.last()
                    Next.dispatch(Effects.effects(
                        GameEffect.SaveRound(
                            model.settings.id!!,
                            Round(
                                null,
                                lastRound.scores[(lastRound.scores.indexOfFirst { it.player == lastRound.dealer } + 1) % lastRound.scores.size].player,
                                lastRound.number + 1,
                                lastRound.scores.map { Score(player = it.player) }
                            )
                        )
                    ))
                }
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
                is GameEvent.ScoreSaved -> {
                    val rounds = model.rounds.toMutableList()
                    val index = rounds.indexOfFirst { it.id == event.roundId }
                    val round = rounds[index].let {
                        val scores = it.scores.toMutableList()
                        val scoreIndex = scores.indexOfFirst { score -> score.id == event.score.id }

                        scores.removeAt(scoreIndex)
                        scores.add(scoreIndex, event.score)
                        it.copy(scores = scores)
                    }

                    rounds.removeAt(index)
                    rounds.add(index, round)
                    Next.next(model.copy(rounds = rounds))
                }
                is GameEvent.UpdateScore -> {
                    val round = model.rounds.firstOrNull { it.id == event.roundId }
                    val score = round?.scores?.firstOrNull { it.player.id == event.playerId }?.copy(value = event.score)

                    if (score != null) {
                        Next.dispatch(
                            Effects.effects(
                                GameEffect.SaveScore(
                                    event.roundId,
                                    score
                                )
                            )
                        )
                    } else {
                        Next.noChange()
                    }
                }
            }
        }
    }
}
