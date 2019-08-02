package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.*

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_4_5 : Migration(4, 5) {
    private val table = "scores"
    private val tempTable = "${table}_temp"
    private val columns = listOf("uid", "player_id", "round_id", "score", "score_data").joinToString(",")
    private val columnsAndData = listOf("uid INTEGER NOT NULL", "player_id INTEGER NOT NULL", "round_id INTEGER NOT NULL", "score REAL NOT NULL", "score_data TEXT NOT NULL").joinToString(",")

    override fun migrate(database: SupportSQLiteDatabase) {
        duplicateScoreTable(database)
    }

    private fun duplicateScoreTable(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `$tempTable`(
            `uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `player_id` INTEGER NOT NULL,
            `round_id` INTEGER NOT NULL,
            `score` REAL NOT NULL,
            `score_data` TEXT NOT NULL,
            FOREIGN KEY(`player_id`) REFERENCES `players`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`round_id`) REFERENCES `rounds`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE)
        """)
        database.execSQL("INSERT INTO $tempTable SELECT $columns FROM $table")
        database.execSQL("DROP TABLE $table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `$table`(
            `uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `player_id` INTEGER NOT NULL,
            `round_id` INTEGER NOT NULL,
            `score` REAL NOT NULL,
            `score_data` TEXT NOT NULL,
            FOREIGN KEY(`player_id`) REFERENCES `players`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`round_id`) REFERENCES `rounds`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )
        """)
        database.execSQL("CREATE  INDEX `index_scores_round_id` ON `$table` (`round_id`)")
        database.execSQL("INSERT INTO $table SELECT $columns FROM $tempTable")
        database.execSQL("DROP TABLE $tempTable")
    }
}