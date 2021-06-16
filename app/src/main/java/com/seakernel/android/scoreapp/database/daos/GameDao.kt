package com.seakernel.android.scoreapp.database.daos

import androidx.room.*
import com.seakernel.android.scoreapp.database.entities.GameEntity
import com.seakernel.android.scoreapp.database.entities.RoundEntity
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface GameDao {
    @Query("SELECT * FROM ${GameEntity.TABLE_NAME} ORDER BY ${GameEntity.COLUMN_LAST_PLAYED} DESC")
    fun getAll(): List<GameEntity>

    @Query("SELECT ${GameEntity.COLUMN_NAME} FROM ${GameEntity.TABLE_NAME} ORDER BY ${GameEntity.COLUMN_NAME} ASC")
    fun getAllNames(): List<String>

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

    @Query("SELECT * FROM ${RoundEntity.TABLE_NAME} WHERE ${RoundEntity.COLUMN_GAME_ID}=:gameId")
    fun getRounds(gameId: Long): List<FulLRoundEntity>

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