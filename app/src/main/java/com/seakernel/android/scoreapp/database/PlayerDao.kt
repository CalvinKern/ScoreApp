package com.seakernel.android.scoreapp.database

import androidx.room.*

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface PlayerDao {
    @Query("SELECT * FROM ${PlayerEntity.TABLE_NAME}")
    fun getAll(): List<PlayerEntity>

    @Query("SELECT * FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_ID} IN (:playerIds)")
    fun loadAllByIds(playerIds: LongArray): List<PlayerEntity>

    @Query("SELECT * FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_NAME} LIKE :name LIMIT 1")
    fun findByName(name: String): PlayerEntity

    @Update
    fun update(player: PlayerEntity)

    @Insert
    fun insertAll(vararg players: PlayerEntity)

    @Delete
    fun delete(player: PlayerEntity)

    @Query("DELETE FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_ID} = :id")
    fun deleteById(id: Long)
}