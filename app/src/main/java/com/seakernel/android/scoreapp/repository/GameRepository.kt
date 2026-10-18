package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.daos.GameDao
import com.seakernel.android.scoreapp.database.entities.GameEntity
import com.seakernel.android.scoreapp.database.entities.GamePlayerJoin
import com.seakernel.android.scoreapp.database.entities.ScoreEntity
import com.seakernel.android.scoreapp.repository.GameRepository.Companion.gameSettingsSaves
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    /**
     * The game's settings, with how many rounds it has. Its first dealer is whoever deals the first round, which can
     * still change while it's the only round (see [updateGame]).
     */
    fun loadGame(gameId: Long): GameSettings? {
        val game = loadFullGame(gameId) ?: return null
        return game.settings.copy(
            roundCount = game.rounds.size,
            initialDealerId = game.rounds.firstOrNull()?.dealer?.id ?: game.settings.initialDealerId,
        )
    }

    /** @return the game with its rounds, or null when it was deleted */
    fun loadFullGame(gameId: Long): Game? {
        val fullGame = gameDao.getFullGame(gameId) ?: return null
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
        // Only a player in the game can deal, anyone else (like a missing dealer saved as 0) would break the round's
        // players foreign key, so someone is picked instead
        val dealerId = settings.initialDealerId?.takeIf { id -> settings.players.any { it.id == id } }
            ?: settings.players.takeIf { it.isNotEmpty() }?.random()?.id
        val dealer = dealerId?.let { Player(id = it) }
        val round = List(settings.players.size) {
            Score(player = Player(settings.players[it].id))
        }
        RoundRepository(context).addOrUpdateRound(id, Round(null, dealer, 0, round))
        id
    }

    /** Saves a game's settings and players, and counts the save in [gameSettingsSaves] once it's done */
    fun updateGame(settings: GameSettings) {
        saveGame(settings)
        settingsSaves.update { it + 1 }
    }

    private fun saveGame(settings: GameSettings) = database.runInTransaction {
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
        // A game still on its first round can change who deals it
        val firstRound = rounds.singleOrNull()?.round
        val dealerId = settings.initialDealerId
        if (firstRound != null && settings.hasDealer && dealerId != null && firstRound.dealerId != dealerId) {
            database.roundDao().updateDealer(firstRound.id, dealerId)
        }

        // Removed players keep their scores, which are hidden while they're out of the game (see convertToScores),
        // so picking them again brings their scores back. New players get a 0 in any round they don't have a score in.
        val roundRepository = RoundRepository(context)
        rounds.forEach { fullRound ->
            roundRepository.insertScores(
                *newPlayers
                .filter { join -> fullRound.scores.none { it.playerId == join.playerId } }
                .map { join -> ScoreEntity(0, join.playerId, fullRound.round.id, 0.0, "") }
                .toTypedArray())
        }
    }

    fun deleteGame(id: Long): Boolean {
        return gameDao.deleteById(id) > 0
    }

    private fun loadPlayers(gameId: Long): List<Player> {
        return gamePlayerDao.getPlayersForGame(gameId).map { player -> Player(player.uid, player.name, color = player.color) }
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

    /** The scores of the players in the game. A player taken out of it keeps theirs, for if they're picked again. */
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

    companion object {
        private val settingsSaves = MutableStateFlow(0L)

        /**
         * How many times any game's settings have been saved. Game Settings saves as it closes, so the game it goes
         * back to can load before the save finishes, and watches this to load again.
         */
        val gameSettingsSaves: StateFlow<Long> = settingsSaves.asStateFlow()
    }
}
