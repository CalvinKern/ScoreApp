package com.seakernel.android.scoreapp.database

import androidx.room.*

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface RoundDao {
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
}