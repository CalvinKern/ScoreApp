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
        return playerDao.getAll().map { it.toPlayer() }.sortedWith(Player.NAME_ORDER)
    }

    fun loadUsers(ids: List<Long>): List<Player> {
        return playerDao.loadAllByIds(ids.toLongArray())
            .map { it.toPlayer() }
            .sortedBy { p -> ids.indexOf(p.id) }
    }

    fun addOrUpdateUser(playerId: Long?, playerName: String, color: Int?): Player {
        val id = if (playerId == null) {
            playerDao.insertAll(arrayOf(PlayerEntity(0, playerName, false, color)))[0]
        } else {
            playerDao.updateNameAndColor(playerId, playerName, color)
            playerId
        }
        return Player(id, playerName, color = color)
    }

    fun deleteUser(id: Long) {
        playerDao.setArchived(id, true)
    }

    fun undoDelete(id: Long) {
        playerDao.setArchived(id, false)
    }

    private fun PlayerEntity.toPlayer() = Player(uid, name, color = color)
}