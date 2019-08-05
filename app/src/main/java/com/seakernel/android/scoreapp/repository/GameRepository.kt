package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.*
import com.seakernel.android.scoreapp.database.*
import org.threeten.bp.ZonedDateTime

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameRepository(val context: Context) {
    private val gameDao = AppDatabase.getInstance(context).gameDao()
    private val gamePlayerDao = AppDatabase.getInstance(context).gamePlayerDao()

    fun loadAllGames(): List<SimpleGame> {
        return gameDao.getAll().map { convertToGame(it) }
    }

    fun loadGame(gameId: Long): SimpleGame? {
        return gameDao.loadAllByIds(longArrayOf(gameId)).firstOrNull()?.let { convertToGame(it) }
    }

    fun loadFullGame(gameId: Long): FullGame {
        val fullGame = gameDao.getFullGame(gameId)
        val game = convertToGame(fullGame.game)
        return FullGame(game, convertToRounds(game, fullGame.rounds))
    }

    /**
     * @return newly created game id
     */
    fun createGame(settings: SimpleGame): Long {
        val game = settings.toGameEntity()
        val id = gameDao.insertAll(game)[0]
        gamePlayerDao.insertAll(*getPlayerJoins(settings.copy(id = id)))

        // Create an empty round for ease
        val dealer = Player(id = settings.initialDealerId ?: settings.players.random().id)
        val round = List(settings.players.size) {
            Score(player = Player(settings.players[it].id))
        }
        RoundRepository(context).addOrUpdateRound(id, Round(null, dealer, 0, round))
        return id
    }

    fun updateGame(settings: SimpleGame) {
        val originalPlayerIds = gamePlayerDao.getPlayersForGame(settings.id!!).map { it.uid }
        val game = settings.toGameEntity()
        gameDao.update(game)

        // Remove players not in the new game
        val currentPlayerIds = settings.players.map { it.id }
        val removedPlayers = originalPlayerIds.filterNot { it in currentPlayerIds }.toLongArray()

        if (removedPlayers.isNotEmpty()) {
            gamePlayerDao.deleteAllPlayersForGame(settings.id, *removedPlayers)
        }

        // Update the players in the new order
        val (oldPlayers, newPlayers) = getPlayerJoins(settings).partition { it.playerId in originalPlayerIds }

        gamePlayerDao.updatePlayerPositions(*oldPlayers.toTypedArray())
        gamePlayerDao.insertAll(*newPlayers.toTypedArray())

        val rounds = gameDao.getRounds(settings.id)
        val roundRepository = RoundRepository(context)
        rounds.forEach { fullRound ->
            // Update old rounds to add players
            roundRepository.insertScores(*newPlayers.map {
                ScoreEntity(0, it.playerId, fullRound.round.id, 0.0, "")
            }.toTypedArray())
            // Remove scores from players not in the game anymore
            removedPlayers.forEach { playerId ->
                fullRound.scores.find { score -> score.playerId == playerId }?.id?.let { scoreId ->
                    roundRepository.deleteScore(scoreId)
                }
            }
        }
    }

    fun deleteGame(id: Long): Boolean {
        return gameDao.deleteById(id) > 0
    }

    private fun loadPlayers(gameId: Long): List<Player> {
        return gamePlayerDao.getPlayersForGame(gameId).map { player -> Player(player.uid, player.name) }
    }

    private fun convertToGame(game: GameEntity): SimpleGame {
        return SimpleGame(game, loadPlayers(game.uid))
    }

    private fun getPlayerJoins(settings: SimpleGame) = settings.players.mapIndexed { index, player ->
        GamePlayerJoin(settings.id!!, player.id!!, index)
    }.toTypedArray()

    private fun convertToRounds(game: SimpleGame, rounds: List<GameDao.FulLRoundEntity>): List<Round> {
        return rounds.map {
            Round(
                it.round.id,
                game.players.find { player -> player.id == it.round.dealerId },
                it.round.roundNumber,
                convertToScores(game, it.scores)
            )
        }
    }

    private fun convertToScores(game: SimpleGame, scores: List<ScoreEntity>): List<Score> {
        // TODO: Could clean up this logic by removing the embedded objects in `GameDao.kt`, and instead using a bigger SQL query
        val playerIds = game.players.map { it.id }
        return scores.sortedBy { playerIds.indexOf(it.playerId) }.map { score ->
            Score(
                score.id,
                game.players.find { player -> player.id == score.playerId }!!,
                score.score,
                score.scoreData
            )
        }
    }
}