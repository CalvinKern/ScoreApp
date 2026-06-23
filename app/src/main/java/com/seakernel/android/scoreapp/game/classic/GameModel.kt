package com.seakernel.android.scoreapp.game.classic

import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.spotify.mobius.Effects
import com.spotify.mobius.Next

/**
 * Created by Calvin on 12/16/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */

sealed class GameEvent {
    data object RequestLoad : GameEvent()
    data object RequestNewGame : GameEvent()
    data object RequestCreateRound : GameEvent()
    data class RequestSaveRound(val round: Round) : GameEvent()
    data class Loaded(val game: Game) : GameEvent()
    data class RoundSaved(val round: Round) : GameEvent()
    data class ScoreSaved(val roundId: Long, val score: Score) : GameEvent()
    data class UpdateScore(
        val roundId: Long,
        val playerId: Long,
        val score: Double,
        val metadata: String
    ) : GameEvent()

    data class ScoreFocused(val scoreId: Long) : GameEvent()
    data class ScoreFocusLost(val scoreId: Long) : GameEvent()
}

sealed class GameEffect {
    data object FetchData : GameEffect()
    data class SaveRound(val gameId: Long, val round: Round) : GameEffect()
    data class SaveScore(val roundId: Long, val score: Score) : GameEffect()
    data class NewGame(val gameId: Long, val initialPlayerId: Long?) : GameEffect()
}

data class GameModel(
    val settings: GameSettings = GameSettings(),
    val rounds: List<Round> = emptyList(),
    val focusedScoreId: Long? = null
) {

    companion object {

        private fun getNextDealer(model: GameModel): Player? {
            if (model.rounds.isEmpty()) return null

            val lastRound = model.rounds.last()
            val nextIndex =
                (lastRound.scores.indexOfFirst { it.player == lastRound.dealer } + 1) % lastRound.scores.size
            return lastRound.scores[nextIndex].player
        }

        fun createDefault(): GameModel {
            return GameModel()
        }

        fun update(model: GameModel, event: GameEvent): Next<GameModel, GameEffect> {
            return when (event) {
                is GameEvent.Loaded -> {
                    val oldFocusedScoreId = model.focusedScoreId
                    val newRounds = event.game.rounds

                    var newFocusedScoreId = oldFocusedScoreId

                    if (oldFocusedScoreId != null && newRounds.none { r -> r.scores.any { it.id == oldFocusedScoreId } }) {
                        // The focused score is gone, find where it used to be
                        var oldScoreIndex = 0
                        val oldFocusedRoundIndex =
                            model.rounds.indexOfFirst { r ->
                                val index = r.scores.indexOfFirst { it.id == oldFocusedScoreId }
                                if (index >= 0) {
                                    oldScoreIndex = index
                                }
                                index >= 0
                            }

                        if (oldFocusedRoundIndex != -1) {
                            // Try to focus the round at the same index in the new list (which is the "next" round)
                            newFocusedScoreId = if (oldFocusedRoundIndex < newRounds.size) {
                                newRounds[oldFocusedRoundIndex].scores.getOrNull(oldScoreIndex)?.id
                            } else if (newRounds.isNotEmpty()) {
                                // If it was the last round, focus the new last round
                                newRounds.last().scores.getOrNull(oldScoreIndex)?.id
                            } else {
                                null
                            }
                        }
                    }

                    Next.next(
                        model.copy(
                            settings = event.game.settings,
                            rounds = newRounds,
                            focusedScoreId = newFocusedScoreId
                        )
                    )
                }
                is GameEvent.RequestLoad -> Next.dispatch(
                    Effects.effects(
                        GameEffect.FetchData
                    )
                )
                is GameEvent.RequestNewGame -> {
                    Next.dispatch(
                        Effects.effects(
                            GameEffect.NewGame(
                                model.settings.id!!,
                                getNextDealer(model)?.id ?: model.settings.initialDealerId
                            )
                        )
                    )
                }
                is GameEvent.RequestSaveRound -> Next.dispatch(
                    Effects.effects(
                        GameEffect.SaveRound(
                            model.settings.id!!,
                            event.round
                        )
                    )
                )
                GameEvent.RequestCreateRound -> {
                    val lastRound = model.rounds.last()
                    Next.dispatch(Effects.effects(
                        GameEffect.SaveRound(
                            model.settings.id!!,
                            Round(
                                null,
                                getNextDealer(model),
                                lastRound.number + 1,
                                lastRound.scores.map { Score(player = it.player) }
                            )
                        )
                    ))
                }
                is GameEvent.RoundSaved -> {
                    val rounds = model.rounds.toMutableList()
                    val index = rounds.indexOfFirst { it.id == event.round.id }
                    val isNewRound = index < 0
                    if (!isNewRound) {
                        rounds.removeAt(index)
                        rounds.add(index, event.round)
                    } else {
                        rounds.add(event.round)
                    }

                    val focusedScoreId = if (isNewRound) {
                        event.round.scores.firstOrNull()?.id
                    } else {
                        model.focusedScoreId
                    }

                    Next.next(model.copy(rounds = rounds, focusedScoreId = focusedScoreId))
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
                    val score = round?.scores?.firstOrNull { it.player.id == event.playerId }
                        ?.copy(value = event.score)

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
                is GameEvent.ScoreFocused -> Next.next(model.copy(focusedScoreId = event.scoreId))
                is GameEvent.ScoreFocusLost -> {
                    if (model.focusedScoreId == event.scoreId) {
                        Next.next(model.copy(focusedScoreId = null))
                    } else {
                        Next.noChange()
                    }
                }
            }
        }
    }
}
