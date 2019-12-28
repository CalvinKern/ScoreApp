package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.entities.RoundEntity
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class RoundRepository(val context: Context) {
    private val roundDao = AppDatabase.getInstance(context).roundDao()

//    fun getNotesForPlayer(player: Player, gameId: Long): List<Round> {
    fun getNotesForPlayer(player: Player, gameId: Long): List<RoundPlayerNote> {
        val rounds = roundDao.getNotesForPlayer(player.id!!, gameId)
        return rounds.map { round ->
            RoundPlayerNote(
                roundId = round.score.roundId,
                roundNumber = round.round_number,
                score = Score(id = round.score.id, player = player, value = round.score.score, metadata = round.score.scoreData)
            )
        }
//        return rounds.map {
//            Round(
//                id = it.round.id,
//                dealer = null,
//                number = it.round.roundNumber,
//                scores = listOf(it.score.let { score ->
//                    Score(
//                        id = score.id,
//                        player = player,
//                        value = score.score,
//                        metadata = score.scoreData
//                    )
//                })
//            )
//        }
    }

    fun addOrUpdateRound(gameId: Long, round: Round): Round {
        return if (round.id == null) {
            createRound(gameId, round)
        } else {
            updateRound(gameId, round)
        }
    }
    fun insertScores(vararg scores: ScoreEntity) {
        roundDao.insertAll(*scores)
    }

    fun updateScores(vararg scores: ScoreEntity) {
        roundDao.update(*scores)
    }

    private fun updateRound(gameId: Long, round: Round): Round {
        roundDao.update(
            RoundEntity(
                round.id!!,
                gameId,
                round.dealer?.id ?: 0,
                round.number
            )
        )
        roundDao.update(*round.scores.map {
            ScoreEntity(
                it.id,
                it.player.id!!,
                round.id,
                it.value,
                it.metadata
            )
        }.toTypedArray())
        return round.copy()
    }

    private fun createRound(gameId: Long, round: Round): Round {
        val roundId = roundDao.insertAll(
            RoundEntity(
                0,
                gameId,
                round.dealer?.id ?: 0,
                round.number
            )
        )[0]
        val scoreIds = roundDao.insertAll(*round.scores.map {
            ScoreEntity(
                0,
                it.player.id!!,
                roundId,
                it.value,
                it.metadata
            )
        }.toTypedArray())
        return round.copy(
            id = roundId,
            scores = round.scores.mapIndexed { index, score -> score.copy(id = scoreIds[index]) }
        )
    }

    fun deleteRound(id: Long) {
        roundDao.deleteRoundById(id)
    }

    fun deleteScore(id: Long) {
        roundDao.deleteScoreById(id)
    }
}

data class RoundPlayerNote(val roundId: Long, val roundNumber: Int, val score: Score)