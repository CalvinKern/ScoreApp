package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_4_5
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.assertEquals
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
class MigrationTest_4_5 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        DbTestHelper.createDatabaseAndMigrate(helper, 4, 5, Migration_4_5()) { db ->
            db.gameDao().getRounds(0).forEach {
                assertEquals(4.toDouble(), it.scores.first().score, 0.01)
            }
        }
    }
}
