package com.seakernel.android.scoreapp.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.seakernel.android.scoreapp.database.entities.GameEntity
import com.seakernel.android.scoreapp.database.entities.GamePlayerJoin
import com.seakernel.android.scoreapp.database.entities.PlayerEntity

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Dao
interface GamePlayerJoinDao {
    @Insert
    fun insertAll(vararg joins: GamePlayerJoin)

    @Query("""
        SELECT ${PlayerEntity.COLUMN_ID}, ${PlayerEntity.COLUMN_NAME}
        FROM ${PlayerEntity.TABLE_NAME}
        INNER JOIN ${GamePlayerJoin.TABLE_NAME}
        ON ${PlayerEntity.COLUMN_ID}=${GamePlayerJoin.COLUMN_PLAYER_ID}
        WHERE ${GamePlayerJoin.COLUMN_GAME_ID}=:gameId
        ORDER BY ${GamePlayerJoin.COLUMN_PLAYER_POSITION}
    """)
    fun getPlayersForGame(gameId: Long): List<PlayerEntity>

    @Query("""
        SELECT ${GameEntity.columnNames}
        FROM ${GameEntity.TABLE_NAME}
        INNER JOIN ${GamePlayerJoin.TABLE_NAME}
        ON ${GameEntity.COLUMN_ID}=${GamePlayerJoin.COLUMN_GAME_ID}
        WHERE ${GamePlayerJoin.COLUMN_PLAYER_ID}=:playerId
        """)
    fun getGamesForPlayer(playerId: Long): List<GameEntity>

    @Update
    fun updatePlayerPositions(vararg joins: GamePlayerJoin)

    @Query("""
       DELETE FROM ${GamePlayerJoin.TABLE_NAME}
       WHERE ${GamePlayerJoin.COLUMN_GAME_ID} = :gameId
       AND ${GamePlayerJoin.COLUMN_PLAYER_ID} IN (:playerIds)
    """)
    fun deleteAllPlayersForGame(gameId: Long, vararg playerIds: Long)
}
