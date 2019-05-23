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
    fun deleteById(id: Long): Int

    @Query("SELECT * FROM ${GameEntity.TABLE_NAME} WHERE ${GameEntity.COLUMN_ID}=:gameId")
    fun getFullGame(gameId: Long): FullGameEntity

    class FullGameEntity {
        @Embedded
        lateinit var game: GameEntity

        @Relation(
            parentColumn = GameEntity.COLUMN_ID,
            entityColumn = RoundEntity.COLUMN_GAME_ID,
            entity = RoundEntity::class
        )
        lateinit var rounds: List<FulLRoundEntity>
    }

    class FulLRoundEntity {
        @Embedded
        lateinit var round: RoundEntity

        @Relation(parentColumn = RoundEntity.COLUMN_ID, entityColumn = ScoreEntity.COLUMN_ROUND_ID)
        lateinit var scores: List<ScoreEntity>
    }
}