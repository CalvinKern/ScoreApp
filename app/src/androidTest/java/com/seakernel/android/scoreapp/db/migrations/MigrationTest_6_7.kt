package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_6_7
import com.seakernel.android.scoreapp.db.DbTestHelper
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
class MigrationTest_6_7 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        val db = DbTestHelper.createDatabaseAndMigrate(helper, 6, 7, Migration_6_7())

        db.gameDao().getFullGame(0).apply {
            assertFalse(game.showRoundNotes)
        }
        db.close()
    }
}
