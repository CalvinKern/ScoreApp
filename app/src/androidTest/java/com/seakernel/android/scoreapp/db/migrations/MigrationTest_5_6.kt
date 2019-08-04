package com.seakernel.android.scoreapp.db.migrations

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.migrations.Migration_5_6
import com.seakernel.android.scoreapp.db.DbTestHelper
import org.junit.Assert.*
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
    fun migrate5To6() {
        val dbVersion = 5
        helper.createDatabase(AppDatabase.database, dbVersion).apply {
            DbTestHelper.generateGameData(dbVersion, this)
            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 6 and provide MIGRATION_5_6 as the migration process
        helper.runMigrationsAndValidate(AppDatabase.database, 6, true, Migration_5_6())

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        val db = getAppDatabaseWithMigrations()

        db.gameDao().getFullGame(0).apply {
            assertFalse(game.showRounds)
            assertFalse(game.reversedScoring)
            assertEquals(null, game.maxScore)
            assertEquals(null, game.maxRounds)
        }
    }
}
