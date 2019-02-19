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

    fun loadAllGames(): List<Game> {
        return gameDao.getAll().map { convertToGame(it) }
    }

    fun loadGame(gameId: Long): Game? {
        return gameDao.loadAllByIds(longArrayOf(gameId)).firstOrNull()?.let { convertToGame(it) }
    }

    fun loadFullGame(gameId: Long): FullGame? {
        val fullGame = gameDao.getFullGame(gameId)
        val game = convertToGame(fullGame.game)
        return FullGame(game, convertToRounds(game, fullGame.rounds))
    }

    /**
     * @return newly created game id
     */
    fun createGame(name: String, playerIds: List<Long>): Long {
        val game = GameEntity(0, name, ZonedDateTime.now().format(Game.DATE_FORMATTER))
        val id = gameDao.insertAll(game)[0]
        gamePlayerDao.insertAll(joins = *playerIds.map { GamePlayerJoin(id, it) }.toTypedArray())
        return id
    }

    fun deleteGame(id: Long) {
        gameDao.deleteById(id)
    }

    private fun loadPlayers(gameId: Long): List<Player> {
        return gamePlayerDao.getPlayersForGame(gameId).map { player -> Player(player.uid, player.name) }
    }

    private fun convertToGame(game: GameEntity): Game {
        return Game(game.uid, game.name, ZonedDateTime.parse(game.date), loadPlayers(game.uid))
    }

    private fun convertToRounds(game: Game, rounds: List<GameDao.FulLRoundEntity>): List<Round> {
        return rounds.map {
            Round(
                it.round.id,
                game.players.find { player -> player.id == it.round.dealerId },
                it.round.roundNumber,
                convertToScores(game, it.scores)
            )
        }
    }

    private fun convertToScores(game: Game, scores: List<ScoreEntity>): List<Score> {
        return scores.map { score ->
            Score(
                score.id,
                game.players.find { player -> player.id == score.playerId }!!,
                score.score,
                score.scoreData
            )
        }
    }
}