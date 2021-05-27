package com.seakernel.android.scoreapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.seakernel.android.scoreapp.database.daos.GameDao
import com.seakernel.android.scoreapp.database.daos.GamePlayerJoinDao
import com.seakernel.android.scoreapp.database.daos.PlayerDao
import com.seakernel.android.scoreapp.database.daos.RoundDao
import com.seakernel.android.scoreapp.database.entities.*
import com.seakernel.android.scoreapp.database.migrations.*

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Database(
    version = 9,
    entities = [PlayerEntity::class, GameEntity::class, GamePlayerJoin::class, RoundEntity::class, ScoreEntity::class]
    )
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gamePlayerDao(): GamePlayerJoinDao
    abstract fun roundDao(): RoundDao

    companion object {
        const val database: String = "score_app"
        private var db: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context, databaseName: String = database): AppDatabase {
            if (db == null) {
                db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                    .addMigrations(*migrations())
                    .build()
            }

            return db!!
        }

        fun clean() {
            db = null
        }

        private fun migrations(): Array<Migration> {
            return arrayOf(
                Migration_1_2(),
                Migration_2_3(),
                Migration_3_4(),
                Migration_4_5(),
                Migration_5_6(),
                Migration_6_7(),
                Migration_7_8(),
                Migration_8_9()
            )
        }
    }
}