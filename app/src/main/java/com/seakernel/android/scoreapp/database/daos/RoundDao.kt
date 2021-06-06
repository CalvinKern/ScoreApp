package com.seakernel.android.scoreapp.database.daos

import androidx.room.*
import com.seakernel.android.scoreapp.database.entities.RoundEntity
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface RoundDao {
    @Query("SELECT ${ScoreEntity.fullyQualifiedColumns}, ${RoundEntity.COLUMN_ROUND_NUMBER} FROM ${ScoreEntity.TABLE_NAME} INNER JOIN ${RoundEntity.TABLE_NAME} as ROUND_ ON ROUND_.${RoundEntity.COLUMN_ID}=${ScoreEntity.TABLE_NAME}.${ScoreEntity.COLUMN_ROUND_ID} WHERE ROUND_.${RoundEntity.COLUMN_GAME_ID}=:gameId AND ${ScoreEntity.COLUMN_PLAYER_ID}=:playerId ORDER BY ${RoundEntity.COLUMN_ROUND_NUMBER} DESC")
    fun getNotesForPlayer(playerId: Long, gameId: Long): List<ScoreNoteEntity>

    @Insert
    fun insertAll(vararg rounds: RoundEntity): LongArray

    @Insert
    fun insertAll(vararg scores: ScoreEntity): LongArray

    @Query("SELECT * FROM ${RoundEntity.TABLE_NAME} WHERE ${RoundEntity.COLUMN_ID} = :id")
    fun getRound(id: Long): RoundEntity

    @Query("SELECT ${RoundEntity.COLUMN_ID} FROM ${RoundEntity.TABLE_NAME} WHERE ${RoundEntity.COLUMN_GAME_ID} = :gameId")
    fun getRoundIds(gameId: Long): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(round: RoundEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg score: ScoreEntity)

    @Query("""
        UPDATE ${ScoreEntity.TABLE_NAME}
        SET ${ScoreEntity.COLUMN_SCORE} = :score
        WHERE ${ScoreEntity.COLUMN_ID} = :id
        
    """)
    fun updateScore(id: Long, score: Double)

    @Query("""
        UPDATE ${ScoreEntity.TABLE_NAME}
        SET ${ScoreEntity.COLUMN_SCORE_DATA} = :notes
        WHERE ${ScoreEntity.COLUMN_ID} = :id
    """)
    fun updateScoreNote(id: Long, notes: String)

    @Query("DELETE FROM ${RoundEntity.TABLE_NAME} WHERE ${RoundEntity.COLUMN_ID} IN (:ids)")
    fun deleteRoundsById(vararg ids: Long)

    @Query("DELETE FROM ${ScoreEntity.TABLE_NAME} WHERE ${ScoreEntity.COLUMN_ID} = :id")
    fun deleteScoreById(id: Long)

    class ScoreNoteEntity {
        @Embedded
        lateinit var score: ScoreEntity

        var round_number: Int = 0
    }

}