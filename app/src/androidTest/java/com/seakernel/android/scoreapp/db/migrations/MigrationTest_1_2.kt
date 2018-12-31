package com.seakernel.android.scoreapp.db.migrations

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.migrations.Migration_1_2
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("ClassName")
class MigrationTest_1_2 {

    @Rule
    @JvmField
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun getAppContext(): Context {
        return ApplicationProvider.getApplicationContext()
    }

    private fun getAppDatabaseWithMigrations(): AppDatabase {
        return AppDatabase.getInstance(getAppContext())
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        val testN = 5
        helper.createDatabase(AppDatabase.database, 1).apply {
            DbTestHelper.generateDataRaw(testN, this)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration process
        helper.runMigrationsAndValidate(AppDatabase.database, 2, true, Migration_1_2())

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        val db = getAppDatabaseWithMigrations()

        assertEquals(testN, db.playerDao().getAll().size)
        assertEquals(testN, db.gameDao().getAll().size)
    }
}
