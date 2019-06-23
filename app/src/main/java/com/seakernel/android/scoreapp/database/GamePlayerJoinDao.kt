package com.seakernel.android.scoreapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

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
        SELECT ${GameEntity.COLUMN_ID}, ${GameEntity.COLUMN_NAME}, ${GameEntity.COLUMN_LAST_PLAYED}
        FROM ${GameEntity.TABLE_NAME}
        INNER JOIN ${GamePlayerJoin.TABLE_NAME}
        ON ${GameEntity.COLUMN_ID}=${GamePlayerJoin.COLUMN_GAME_ID}
        WHERE ${GamePlayerJoin.COLUMN_PLAYER_ID}=:playerId
        """)
    fun getGamesForPlayer(playerId: Long): List<GameEntity>
}