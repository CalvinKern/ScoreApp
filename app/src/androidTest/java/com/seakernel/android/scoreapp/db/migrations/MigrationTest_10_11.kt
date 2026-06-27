package com.seakernel.android.scoreapp.db.migrations

import com.seakernel.android.scoreapp.database.migrations.Migration_10_11
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Instrumented test for migration from version 10 to 11.
 */
@Suppress("ClassName")
class MigrationTest_10_11 {

    @Rule
    @JvmField
    val helper = DbTestHelper.migrationTestHelper

    @Test
    @Throws(IOException::class)
    fun migrate10_11() {
        DbTestHelper.createDatabaseAndMigrate(helper, 10, 11, Migration_10_11()) { db ->
            // MigrationTestHelper.runMigrationsAndValidate already validates the schema (including indices).
            // We can also perform a simple data integrity check if needed.
            val player = db.playerDao().getAll().first()
            assert(player.name.startsWith("User"))
        }
    }
}
