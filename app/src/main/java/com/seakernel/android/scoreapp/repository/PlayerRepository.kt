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

    fun updateUser(player: Player) {
        playerDao.update(PlayerEntity(player.id, player.name))
    }

    fun creatUser(name: String): Player {
        val player = PlayerEntity(0, name)
        playerDao.insertAll(player)
        return Player(player.uid, name)
    }

    fun deleteUser(id: Long) {
        playerDao.deleteById(id)
    }
}