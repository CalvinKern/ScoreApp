package com.seakernel.android.scoreapp.database

import androidx.room.*

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

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(round: RoundEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg score: ScoreEntity)

    @Query("DELETE FROM ${RoundEntity.TABLE_NAME} WHERE ${RoundEntity.COLUMN_ID} = :id")
    fun deleteRoundById(id: Long)

    @Query("DELETE FROM ${ScoreEntity.TABLE_NAME} WHERE ${ScoreEntity.COLUMN_ID} = :id")
    fun deleteScoreById(id: Long)

    class ScoreNoteEntity {
        @Embedded
        lateinit var score: ScoreEntity

        var round_number: Int = 0
    }

    data class RoundNotes(val round: RoundEntity, val scores: List<ScoreEntity>)
    class RoundNoteEntity {
        @Embedded(prefix = ScoreEntity.TABLE_NAME)
        lateinit var score: ScoreEntity
//        @Relation(parentColumn = RoundEntity.COLUMN_ID, entityColumn = ScoreEntity.COLUMN_ROUND_ID)
//        lateinit var scores: List<ScoreEntity>

        @Embedded(prefix = RoundEntity.TABLE_NAME)
        lateinit var round: RoundEntity
    }
}