package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_5_6
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("ClassName")
class MigrationTest_5_6 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        val db = DbTestHelper.createDatabaseAndMigrate(helper, 5, 6, Migration_5_6())

        db.gameDao().getFullGame(0).apply {
            assertFalse(game.showRounds)
            assertFalse(game.reversedScoring)
            assertEquals(null, game.maxScore)
            assertEquals(null, game.maxRounds)
        }
    }
}
