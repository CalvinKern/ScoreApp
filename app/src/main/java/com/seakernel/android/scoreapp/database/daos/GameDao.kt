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
    @Query(
        """
        SELECT ${GameEntity.TABLE_NAME}.*, (
            SELECT COUNT(*) FROM ${RoundEntity.TABLE_NAME}
            WHERE ${RoundEntity.TABLE_NAME}.${RoundEntity.COLUMN_GAME_ID} = ${GameEntity.TABLE_NAME}.${GameEntity.COLUMN_ID}
        ) AS ${GameWithRoundCount.COLUMN_ROUND_COUNT}
        FROM ${GameEntity.TABLE_NAME}
        ORDER BY ${GameEntity.COLUMN_LAST_PLAYED} DESC
    """
    )
    fun getAllWithRoundCount(): List<GameWithRoundCount>

    @Query("SELECT ${GameEntity.COLUMN_NAME} FROM ${GameEntity.TABLE_NAME} ORDER BY ${GameEntity.COLUMN_NAME} COLLATE NOCASE ASC")
    fun getAllNames(): List<String>

    @Query("SELECT * FROM ${GameEntity.TABLE_NAME} WHERE TRIM(${GameEntity.COLUMN_NAME}) = TRIM(:name) ORDER BY ${GameEntity.COLUMN_LAST_PLAYED} DESC LIMIT 1")
    fun getMostRecentByName(name: String): GameEntity?

    @Query("SELECT * FROM ${GameEntity.TABLE_NAME} WHERE ${GameEntity.COLUMN_ID} IN (:gameIds)")
    fun loadAllByIds(gameIds: LongArray): List<GameEntity>

    @Update
    fun update(game: GameEntity)

    @Insert
    fun insertAll(games: Array<GameEntity>): LongArray

    @Delete
    fun delete(game: GameEntity)

    @Query("DELETE FROM ${GameEntity.TABLE_NAME} WHERE ${GameEntity.COLUMN_ID} = :id")
    fun deleteById(id: Long): Int

    @Transaction
    @Query("SELECT * FROM ${GameEntity.TABLE_NAME} WHERE ${GameEntity.COLUMN_ID}=:gameId")
    fun getFullGame(gameId: Long): FullGameEntity? // Null when the game was deleted

    @Transaction
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

    class GameWithRoundCount {
        @Embedded
        lateinit var game: GameEntity

        @ColumnInfo(name = COLUMN_ROUND_COUNT)
        var roundCount: Int = 0

        companion object {
            const val COLUMN_ROUND_COUNT = "round_count"
        }
    }
}