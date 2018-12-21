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

    @Update
    fun update(player: PlayerEntity)

    @Insert
    fun insertAll(vararg players: PlayerEntity): LongArray

    @Delete
    fun delete(player: PlayerEntity)

    @Query("DELETE FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_ID} = :id")
    fun deleteById(id: Long)
}