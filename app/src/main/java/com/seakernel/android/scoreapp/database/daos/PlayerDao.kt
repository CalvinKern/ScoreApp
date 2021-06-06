package com.seakernel.android.scoreapp.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.seakernel.android.scoreapp.database.entities.PlayerEntity

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface PlayerDao {
    @Query("SELECT * FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_ARCHIVED}=0 ORDER BY ${PlayerEntity.COLUMN_NAME}")
    fun getAll(): List<PlayerEntity>

    @Query("SELECT * FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_ID} IN (:playerIds)")
    fun loadAllByIds(playerIds: LongArray): List<PlayerEntity>

    @Update
    fun update(player: PlayerEntity)

    @Query("""
        UPDATE ${PlayerEntity.TABLE_NAME}
        SET ${PlayerEntity.COLUMN_NAME} = :name
        WHERE ${PlayerEntity.COLUMN_ID} = :id
    """)
    fun updateName(id: Long, name: String)

    @Insert
    fun insertAll(vararg players: PlayerEntity): LongArray

    @Query("DELETE FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_ID} = :id")
    fun deleteById(id: Long)

    @Query("""
        Update ${PlayerEntity.TABLE_NAME}
        SET ${PlayerEntity.COLUMN_ARCHIVED} = :archive
        WHERE ${PlayerEntity.COLUMN_ID} = :id
    """)
    fun setArchived(id: Long, archive: Boolean)
}
