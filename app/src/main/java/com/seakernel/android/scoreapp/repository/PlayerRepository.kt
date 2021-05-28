package com.seakernel.android.scoreapp.repository

import android.content.Context
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.daos.PlayerDao
import com.seakernel.android.scoreapp.database.entities.PlayerEntity

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class PlayerRepository(val context: Context) {
    private val playerDao: PlayerDao = AppDatabase.getInstance(context).playerDao()

    fun loadAllUsers(): List<Player> {
        return playerDao.getAll().map { Player(it.uid, it.name) }
    }

    fun loadUsers(ids: List<Long>): List<Player> {
        return playerDao.loadAllByIds(ids.toLongArray())
            .map { Player(it.uid, it.name) }
            .sortedBy { p -> ids.indexOf(p.id) }
    }

    fun addOrUpdateUser(playerId: Long?, playerName: String): Player {
        return if (playerId == null) {
            createUser(playerName)
        } else {
            updateUser(playerId, playerName)
        }
    }

    private fun updateUser(playerId: Long, playerName: String): Player {
        playerDao.updateName(playerId, playerName)
        return Player(playerId, playerName)
    }

    private fun createUser(name: String): Player {
        val player =
            PlayerEntity(
                0,
                name,
                false,
            )
        val id = playerDao.insertAll(player)[0]
        return Player(id, name)
    }

    fun deleteUser(id: Long) {
        playerDao.setArchived(id, true)
    }
}