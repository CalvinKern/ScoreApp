package com.seakernel.android.scoreapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.seakernel.android.scoreapp.database.migrations.Migration_1_2

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Database(entities = [PlayerEntity::class, GameEntity::class, GamePlayerJoin::class, RoundEntity::class, ScoreEntity::class], version = 2)
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
            return arrayOf(Migration_1_2())
        }
    }
}