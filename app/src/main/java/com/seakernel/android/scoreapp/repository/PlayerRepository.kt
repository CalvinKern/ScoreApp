package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.PlayerDao
import com.seakernel.android.scoreapp.database.PlayerEntity

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class PlayerRepository(val context: Context) {
    private val playerDao: PlayerDao = AppDatabase.getInstance(context).playerDao()

    fun loadAllUsers(): List<Player> {
        return playerDao.getAll().map { Player(it.uid, it.name) }
    }

    fun loadUsers(ids: List<Long>) : List<Player> {
        return playerDao.loadAllByIds(ids.toLongArray()).map { Player(it.uid, it.name) }
    }

    fun addOrUpdateUser(playerId: Long, playerName: String): Player {
        return if (playerId == 0L) { // treat 0 as not-set while inserting the item
            createUser(playerName)
        } else {
            updateUser(playerId, playerName)
        }
    }

    private fun updateUser(playerId: Long, playerName: String): Player {
        playerDao.update(PlayerEntity(playerId, playerName))
        return Player(playerId, playerName)
    }

    private fun createUser(name: String): Player {
        val player = PlayerEntity(0, name)
        val id = playerDao.insertAll(player)[0]
        return Player(id, name)
    }

    fun deleteUser(id: Long) {
        playerDao.deleteById(id)
    }
}