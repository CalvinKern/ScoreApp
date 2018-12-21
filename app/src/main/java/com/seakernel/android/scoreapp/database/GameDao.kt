package com.seakernel.android.scoreapp.database

import androidx.room.*

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface GameDao {
    @Query("SELECT * FROM ${GameEntity.TABLE_NAME}")
    fun getAll(): List<GameEntity>

    @Query("SELECT * FROM ${GameEntity.TABLE_NAME} WHERE ${GameEntity.COLUMN_ID} IN (:gameIds)")
    fun loadAllByIds(gameIds: LongArray): List<GameEntity>

    @Update
    fun update(game: GameEntity)

    @Insert
    fun insertAll(vararg games: GameEntity): LongArray

    @Delete
    fun delete(game: GameEntity)

    @Query("DELETE FROM ${GameEntity.TABLE_NAME} WHERE ${GameEntity.COLUMN_ID} = :id")
    fun deleteById(id: Long)
}