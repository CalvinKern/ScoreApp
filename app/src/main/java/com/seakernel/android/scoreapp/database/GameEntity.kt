package com.seakernel.android.scoreapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Entity(tableName = GameEntity.TABLE_NAME)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID, index = true) var uid: Long,
    @ColumnInfo(name = COLUMN_NAME) var name: String,
    @ColumnInfo(name = COLUMN_LAST_PLAYED) var date: String
) {
    companion object {
        const val TABLE_NAME = "games"
        const val COLUMN_ID = "uid"
        const val COLUMN_NAME = "name"
        const val COLUMN_LAST_PLAYED = "date"
    }
}