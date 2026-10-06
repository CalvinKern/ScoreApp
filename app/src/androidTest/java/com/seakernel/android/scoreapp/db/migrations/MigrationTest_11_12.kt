package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.entities.ScoreEntity
import com.seakernel.android.scoreapp.database.migrations.Migration_11_12
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Instrumented test for migration from version 11 to 12, which adds the typed input for scores.
 */
@Suppress("ClassName")
class MigrationTest_11_12 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate11_12() {
        DbTestHelper.createDatabaseAndMigrate(helper, 11, 12, Migration_11_12()) { db ->
            // runMigrationsAndValidate already validates the schema, existing scores should have no input yet
            db.openHelper.readableDatabase.query("SELECT COUNT(*), COUNT(${ScoreEntity.COLUMN_SCORE_INPUT}) FROM ${ScoreEntity.TABLE_NAME}").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) > 0)
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate11_12_whenColumnAlreadyExists() {
        DbTestHelper.createDatabaseAndMigrate(helper, 11, 12, Migration_11_12()) { db ->
            val database = db.openHelper.writableDatabase
            database.execSQL("UPDATE ${ScoreEntity.TABLE_NAME} SET ${ScoreEntity.COLUMN_SCORE_INPUT} = '1+1'")

            // Running it again skips adding the column instead of failing, and keeps what was stored in it
            Migration_11_12().migrate(database)

            database.query("SELECT COUNT(*) FROM ${ScoreEntity.TABLE_NAME} WHERE ${ScoreEntity.COLUMN_SCORE_INPUT} = '1+1'").use { cursor ->
                cursor.moveToFirst()
                assert(cursor.getInt(0) > 0)
            }
        }
    }
}
