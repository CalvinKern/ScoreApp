package com.seakernel.android.scoreapp.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.seakernel.android.scoreapp.data.SimpleGame
import com.seakernel.android.scoreapp.database.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("ClassName", "UNUSED_VARIABLE")
class DbTestHelper {

    private lateinit var playerDao: PlayerDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            getAppContext(), AppDatabase::class.java).build()
        playerDao = db.playerDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertRawUsers() {
        val n = 5
        insertUsersRaw(n, db.openHelper.writableDatabase)

        val allPlayers = playerDao.getAll()
        assertEquals(n, allPlayers.size)
    }

    companion object {
        private const val databaseName = "${AppDatabase.database}_test"

        val migrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
        )

        private fun getAppContext(): Context {
            return ApplicationProvider.getApplicationContext()
        }

        private fun getAppDatabaseWithMigrations(): AppDatabase {
            return AppDatabase.getInstance(getAppContext(), databaseName)
        }

        fun createDatabaseAndMigrate(helper: MigrationTestHelper, startDbVersion: Int, endDbVersion: Int, migration: Migration): AppDatabase {
            helper.createDatabase(databaseName, startDbVersion).apply {
                generateGameData(startDbVersion, this)
                // Prepare for the next version.
                close()
            }

            // Re-open the database with the end version and provide the migration
            helper.runMigrationsAndValidate(databaseName, endDbVersion, true, migration)

            // MigrationTestHelper automatically verifies the schema changes,
            // but you need to validate that the data was migrated properly.
            return getAppDatabaseWithMigrations()
        }

        fun now(): String {
            return ZonedDateTime.now().format(SimpleGame.DATE_FORMATTER)
        }

        fun generateGameData(version: Int, db: SupportSQLiteDatabase) {
            val testN = 1
            if (version >= 4) {
                when {
                    version >= 7 -> insertGamesV7(testN, db)
                    version >= 6 -> insertGamesV6(testN, db)
                    else -> insertGamesV4(testN, db)
                }
                insertUsersRaw(testN, db)
                insertGamePlayersV4(testN, db)
                insertScoresV4(version, testN, db)
            }
        }

        private fun insertScoresV4(score: Int, n: Int, db: SupportSQLiteDatabase) {
            val rounds = List(n) { "($it, $it, 0, 0)" }.joinToString(",")
            db.execSQL("INSERT INTO ${RoundEntity.TABLE_NAME} " +
                "(${RoundEntity.COLUMN_ID}, ${RoundEntity.COLUMN_ROUND_NUMBER}, ${RoundEntity.COLUMN_GAME_ID}, ${RoundEntity.COLUMN_DEALER_ID}) " +
                "VALUES $rounds")

            val scores = List(n) { "($it, 0, 0, $score, '')" }.joinToString(",")
            db.execSQL("INSERT INTO ${ScoreEntity.TABLE_NAME} " +
                "(${ScoreEntity.COLUMN_ID}, ${ScoreEntity.COLUMN_ROUND_ID}, ${ScoreEntity.COLUMN_PLAYER_ID}, ${ScoreEntity.COLUMN_SCORE}, ${ScoreEntity.COLUMN_SCORE_DATA}) " +
                "VALUES $scores")
        }

        private fun insertGamePlayersV4(n: Int, db: SupportSQLiteDatabase) {
            var gamePlayers = ""
            repeat(n) {
                gamePlayers = gamePlayers.plus("(0, $it, $it)")
                if (it < n - 1) {
                    gamePlayers = gamePlayers.plus(", ")
                }
            }
            db.execSQL("INSERT INTO ${GamePlayerJoin.TABLE_NAME} " +
                "(${GamePlayerJoin.COLUMN_GAME_ID}, ${GamePlayerJoin.COLUMN_PLAYER_ID}, ${GamePlayerJoin.COLUMN_PLAYER_POSITION}) " +
                "VALUES $gamePlayers")
        }

        private fun insertGamesV7(n: Int, db: SupportSQLiteDatabase) {
            val gameValues = List(n) { "($it, \"SimpleGame $it\", \"${now()}\", 0, 0, 0, 0, 0, 0)" }
            val gameEntityColumnsV7 =
                "(${GameEntity.COLUMN_ID}, ${GameEntity.COLUMN_NAME}, ${GameEntity.COLUMN_LAST_PLAYED}, ${GameEntity.COLUMN_HAS_DEALER}, ${GameEntity.COLUMN_SHOW_ROUNDS}, ${GameEntity.COLUMN_REVERSED_SCORING}, ${GameEntity.COLUMN_MAX_SCORE}, ${GameEntity.COLUMN_MAX_ROUNDS}, ${GameEntity.COLUMN_SHOW_ROUND_NOTES})"

            db.execSQL("INSERT INTO ${GameEntity.TABLE_NAME} $gameEntityColumnsV7 VALUES ${gameValues.joinToString()}")
        }

        private fun insertGamesV6(n: Int, db: SupportSQLiteDatabase) {
            val gameValues = List(n) { "($it, \"SimpleGame $it\", \"${now()}\", 0, 0, 0, 0, 0)" }
            val gameEntityColumnsV6 =
                "(${GameEntity.COLUMN_ID}, ${GameEntity.COLUMN_NAME}, ${GameEntity.COLUMN_LAST_PLAYED}, ${GameEntity.COLUMN_HAS_DEALER}, ${GameEntity.COLUMN_SHOW_ROUNDS}, ${GameEntity.COLUMN_REVERSED_SCORING}, ${GameEntity.COLUMN_MAX_SCORE}, ${GameEntity.COLUMN_MAX_ROUNDS})"

            db.execSQL("INSERT INTO ${GameEntity.TABLE_NAME} $gameEntityColumnsV6 VALUES ${gameValues.joinToString()}")
        }

        private fun insertGamesV4(n: Int, db: SupportSQLiteDatabase) {
            var gameValues = ""
            repeat(n) {
                gameValues = gameValues.plus("($it, \"SimpleGame $it\", \"${now()}\", 1)")
                if (it < n - 1) {
                    gameValues = gameValues.plus(", ")
                }
            }

            db.execSQL("INSERT INTO ${GameEntity.TABLE_NAME} " +
                "(${GameEntity.COLUMN_ID}, ${GameEntity.COLUMN_NAME}, ${GameEntity.COLUMN_LAST_PLAYED}, ${GameEntity.COLUMN_HAS_DEALER}) " +
                "VALUES $gameValues")
        }

        fun insertUsersRaw(n: Int, db: SupportSQLiteDatabase) {
            var userValues = ""
            repeat(n) {
                userValues = userValues.plus("($it, \"User $it\")")
                if (it < n - 1) {
                    userValues = userValues.plus(", ")
                }
            }
            db.execSQL("INSERT INTO ${PlayerEntity.TABLE_NAME} " +
                    "(${PlayerEntity.COLUMN_ID}, ${PlayerEntity.COLUMN_NAME}) " +
                    "VALUES $userValues")
        }
    }
}
