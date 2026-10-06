package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.*
import com.seakernel.android.scoreapp.database.*
import com.seakernel.android.scoreapp.database.daos.GameDao
import com.seakernel.android.scoreapp.database.entities.GameEntity
import com.seakernel.android.scoreapp.database.entities.GamePlayerJoin
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameRepository(val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val gameDao = database.gameDao()
    private val gamePlayerDao = database.gamePlayerDao()

    fun loadAllGames(): List<GameSettings> {
        return gameDao.getAllWithRoundCount().map {
            convertToGame(it.game).copy(roundCount = it.roundCount)
        }
    }

    fun gameNameSearch(): List<String> {
        return gameDao.getAllNames()
    }

    fun loadMostRecentGame(name: String): GameSettings? {
        return gameDao.getMostRecentByName(name)?.let { convertToGame(it) }
    }

    fun loadGame(gameId: Long): GameSettings? {
        return gameDao.loadAllByIds(longArrayOf(gameId)).firstOrNull()?.let { convertToGame(it) }
    }

    fun loadFullGame(gameId: Long): Game {
        val fullGame = gameDao.getFullGame(gameId)
        val game = convertToGame(fullGame.game)
        return Game(game, convertToRounds(game, fullGame.rounds))
    }

    /**
     * @return newly created game id
     */
    fun createGame(settings: GameSettings): Long = database.runInTransaction<Long> {
        // All or nothing, so a failure can't leave a game without its players or first round
        val game = settings.toGameEntity()
        val id = gameDao.insertAll(arrayOf(game))[0]
        gamePlayerDao.insertAll(getPlayerJoins(settings.copy(id = id)))

        // Create an empty round for ease
        val dealerId = settings.initialDealerId ?: settings.players.takeIf { it.isNotEmpty() }?.random()?.id
        val dealer = dealerId?.let { Player(id = it) }
        val round = List(settings.players.size) {
            Score(player = Player(settings.players[it].id))
        }
        RoundRepository(context).addOrUpdateRound(id, Round(null, dealer, 0, round))
        id
    }

    fun updateGame(settings: GameSettings) = database.runInTransaction(Runnable {
        // All or nothing, so a failure can't leave players without scores for some rounds
        val originalPlayerIds = gamePlayerDao.getPlayersForGame(settings.id!!).map { it.uid }
        val game = settings.toGameEntity()
        gameDao.update(game)

        // Remove players not in the new game
        val currentPlayerIds = settings.players.map { it.id }
        val removedPlayers = originalPlayerIds.filterNot { it in currentPlayerIds }.toLongArray()

        if (removedPlayers.isNotEmpty()) {
            gamePlayerDao.deleteAllPlayersForGame(settings.id, removedPlayers)
        }

        // Update the players in the new order
        val (oldPlayers, newPlayers) = getPlayerJoins(settings).partition { it.playerId in originalPlayerIds }

        gamePlayerDao.updatePlayerPositions(oldPlayers.toTypedArray())
        gamePlayerDao.insertAll(newPlayers.toTypedArray())

        val rounds = gameDao.getRounds(settings.id)
        val roundRepository = RoundRepository(context)
        rounds.forEach { fullRound ->
            // Update old rounds to add players
            roundRepository.insertScores(*newPlayers.map {
                ScoreEntity(
                    0,
                    it.playerId,
                    fullRound.round.id,
                    0.0,
                    ""
                )
            }.toTypedArray())
            // Remove scores from players not in the game anymore
            removedPlayers.forEach { playerId ->
                fullRound.scores.find { score -> score.playerId == playerId }?.id?.let { scoreId ->
                    roundRepository.deleteScore(scoreId)
                }
            }
        }
    })

    fun deleteGame(id: Long): Boolean {
        return gameDao.deleteById(id) > 0
    }

    private fun loadPlayers(gameId: Long): List<Player> {
        return gamePlayerDao.getPlayersForGame(gameId).map { player -> Player(player.uid, player.name) }
    }

    private fun convertToGame(game: GameEntity): GameSettings {
        return GameSettings(game, loadPlayers(game.uid))
    }

    private fun getPlayerJoins(settings: GameSettings) = settings.players.mapIndexed { index, player ->
        GamePlayerJoin(
            settings.id!!,
            player.id!!,
            index
        )
    }.toTypedArray()

    private fun convertToRounds(settings: GameSettings, rounds: List<GameDao.FulLRoundEntity>): List<Round> {
        return rounds
            .sortedWith(compareBy({ it.round.roundNumber }, { it.round.id })) // The order they were added in
            .map {
                Round(
                    it.round.id,
                    settings.players.find { player -> player.id == it.round.dealerId },
                    it.round.roundNumber,
                    convertToScores(settings, it.scores)
                )
            }
    }

    private fun convertToScores(settings: GameSettings, scores: List<ScoreEntity>): List<Score> {
        // TODO: Could clean up this logic by removing the embedded objects in `GameDao.kt`, and instead using a bigger SQL query
        val playerIds = settings.players.map { it.id }
        return scores.sortedBy { playerIds.indexOf(it.playerId) }.mapNotNull { score ->
            val player = settings.players.find { player -> player.id == score.playerId } ?: return@mapNotNull null
            Score(
                score.id,
                player,
                score.score,
                score.scoreData,
                score.scoreInput,
            )
        }
    }
}