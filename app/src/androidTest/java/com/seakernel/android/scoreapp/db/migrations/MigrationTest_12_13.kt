package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.entities.PlayerEntity
import com.seakernel.android.scoreapp.database.migrations.Migration_12_13
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Instrumented test for migration from version 12 to 13, which adds a color for each player.
 */
@Suppress("ClassName")
class MigrationTest_12_13 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate12_13() {
        DbTestHelper.createDatabaseAndMigrate(helper, 12, 13, Migration_12_13()) { db ->
            // runMigrationsAndValidate already validates the schema, existing players should have no color yet
            db.openHelper.readableDatabase.query("SELECT COUNT(*), COUNT(${PlayerEntity.COLUMN_COLOR}) FROM ${PlayerEntity.TABLE_NAME}").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) > 0)
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate12_13_whenColumnAlreadyExists() {
        DbTestHelper.createDatabaseAndMigrate(helper, 12, 13, Migration_12_13()) { db ->
            val database = db.openHelper.writableDatabase
            database.execSQL("UPDATE ${PlayerEntity.TABLE_NAME} SET ${PlayerEntity.COLUMN_COLOR} = 255")

            // Running it again skips adding the column instead of failing, and keeps what was stored in it
            Migration_12_13().migrate(database)

            database.query("SELECT COUNT(*) FROM ${PlayerEntity.TABLE_NAME} WHERE ${PlayerEntity.COLUMN_COLOR} = 255").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) > 0)
            }
        }
    }
}
