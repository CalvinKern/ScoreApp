package com.seakernel.android.scoreapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Entity(tableName = PlayerEntity.TABLE_NAME)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID, index = true) var uid: Long,
    @ColumnInfo(name = COLUMN_NAME) var name: String
) {
    companion object {
        const val TABLE_NAME = "players"
        const val COLUMN_ID = "uid"
        const val COLUMN_NAME = "name"
    }
}