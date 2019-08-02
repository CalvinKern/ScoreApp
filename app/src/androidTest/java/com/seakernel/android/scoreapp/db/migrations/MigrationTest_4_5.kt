package com.seakernel.android.scoreapp.db.migrations

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.migrations.Migration_4_5
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
class MigrationTest_4_5 {

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
    fun migrate4To5() {
        val dbVersion = 4
        helper.createDatabase(AppDatabase.database, dbVersion).apply {
            DbTestHelper.generateGameData(dbVersion, this)
            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 5 and provide MIGRATION_4_5 as the migration process
        helper.runMigrationsAndValidate(AppDatabase.database, 5, true, Migration_4_5())

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        val db = getAppDatabaseWithMigrations()

        db.gameDao().getRounds(0).forEach {
            assertEquals(dbVersion.toDouble(), it.scores.first().score, 0.01)
        }
    }
}
