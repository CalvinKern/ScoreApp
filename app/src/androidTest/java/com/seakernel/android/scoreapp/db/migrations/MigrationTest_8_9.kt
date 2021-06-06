package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_8_9
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
class MigrationTest_8_9 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        DbTestHelper.createDatabaseAndMigrate(helper, 8, 9, Migration_8_9()) { db ->
            db.playerDao().deleteById(0)
            db.gameDao().getFullGame(0).apply {
                assertTrue(rounds.size == 2)
            }
        }
    }
}
