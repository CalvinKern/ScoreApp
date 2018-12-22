package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.GameEntity
import com.seakernel.android.scoreapp.database.GamePlayerJoin
import org.threeten.bp.ZonedDateTime

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameRepository(val context: Context) {
    private val gameDao = AppDatabase.getInstance(context).gameDao()
    private val gamePlayerDao = AppDatabase.getInstance(context).gamePlayerDao()

    fun loadAllGames(): List<Game> {
        return gameDao.getAll().map { game ->
            Game(game.uid,
                game.name,
                ZonedDateTime.parse(game.date),
                gamePlayerDao.getPlayersForGame(game.uid).map { player -> Player(player.uid, player.name) })
        }
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
}