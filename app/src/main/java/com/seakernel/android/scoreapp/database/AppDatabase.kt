package com.seakernel.android.scoreapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.seakernel.android.scoreapp.database.migrations.Migration_1_2
import com.seakernel.android.scoreapp.database.migrations.Migration_2_3
import com.seakernel.android.scoreapp.database.migrations.Migration_3_4
import com.seakernel.android.scoreapp.database.migrations.Migration_4_5

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Database(
    version = 5,
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
        fun getInstance(context: Context): AppDatabase {
            if (db == null) {
                db = Room.databaseBuilder(context, AppDatabase::class.java, database)
                    .addMigrations(*migrations())
                    .build()
            }

            return db!!
        }

        private fun migrations(): Array<Migration> {
            return arrayOf(Migration_1_2(), Migration_2_3(), Migration_3_4(), Migration_4_5())
        }
    }
}