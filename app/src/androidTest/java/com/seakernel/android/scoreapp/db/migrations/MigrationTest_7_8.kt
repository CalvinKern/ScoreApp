package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_7_8
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("ClassName")
class MigrationTest_7_8 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        DbTestHelper.createDatabaseAndMigrate(helper, 7, 8, Migration_7_8()) { db ->
            db.gameDao().getFullGame(0).apply {
                assertTrue(game.useCalculator)
            }
        }
    }
}
