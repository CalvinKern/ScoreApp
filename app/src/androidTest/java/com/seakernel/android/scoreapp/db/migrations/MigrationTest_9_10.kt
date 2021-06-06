package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_9_10
import com.seakernel.android.scoreapp.db.DbTestHelper
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
class MigrationTest_9_10 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate9_10() {
        DbTestHelper.createDatabaseAndMigrate(helper, 9, 10, Migration_9_10()) { db ->
            db.playerDao().getAll().first().archived = false
        }
    }
}
