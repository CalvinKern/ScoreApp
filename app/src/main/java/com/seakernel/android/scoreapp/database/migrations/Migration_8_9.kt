package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.GameEntity
import com.seakernel.android.scoreapp.database.entities.RoundEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_8_9 : Migration(8, 9) {

    private val table = "rounds"
    private val tempTable = "${table}_temp"
    private val columns = listOf("uid", "game_id", "round_number", "dealer_id").joinToString(",")

    override fun migrate(database: SupportSQLiteDatabase) {
        duplicateRoundTable(database)
    }

    private fun duplicateRoundTable(database: SupportSQLiteDatabase) {
        createTableSql(database, tempTable)
        database.execSQL("INSERT INTO $tempTable SELECT $columns FROM $table")
        database.execSQL("DROP TABLE $table")

        createTableSql(database, table)
        database.execSQL("CREATE  INDEX `index_rounds_game_id` ON `$table` (`game_id`)")
        database.execSQL("INSERT INTO $table SELECT $columns FROM $tempTable")
        database.execSQL("DROP TABLE $tempTable")
    }

    private fun createTableSql(database: SupportSQLiteDatabase, tableName: String) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `$tableName`(
            `uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `game_id` INTEGER NOT NULL,
            `round_number` INTEGER NOT NULL,
            `dealer_id` INTEGER,
            FOREIGN KEY(`game_id`) REFERENCES `games`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`dealer_id`) REFERENCES `players`(`uid`) ON UPDATE NO ACTION ON DELETE SET NULL)
        """)
    }
}