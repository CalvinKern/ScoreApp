package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.RoundEntity
import com.seakernel.android.scoreapp.database.ScoreEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class RoundRepository(val context: Context) {
    private val roundDao = AppDatabase.getInstance(context).roundDao()

    fun addOrUpdateRound(gameId: Long, round: Round): Round {
        return if (round.id == 0L) { // treat 0 as not-set while inserting items
            createRound(gameId, round)
        } else {
            updateRound(gameId, round)
        }
    }

    private fun updateRound(gameId: Long, round: Round): Round {
        roundDao.update(RoundEntity(round.id, gameId, round.dealer?.id ?: 0, round.number))
        roundDao.update(*round.scores.map {
            ScoreEntity(it.id, it.player.id, round.id, it.value, it.metadata)
        }.toTypedArray())
        return round.copy()
    }

    private fun createRound(gameId: Long, round: Round): Round {
        val roundId = roundDao.insertAll(
            RoundEntity(0, gameId, round.dealer?.id ?: 0, round.number)
        )[0]
        val scoreIds = roundDao.insertAll(*round.scores.map {
            ScoreEntity(0, it.player.id, roundId, it.value, it.metadata)
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