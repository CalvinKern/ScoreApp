package com.seakernel.android.scoreapp.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.database.*
import com.seakernel.android.scoreapp.database.daos.PlayerDao
import com.seakernel.android.scoreapp.database.entities.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.io.IOException
import kotlin.jvm.Throws

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

        fun createDatabaseAndMigrate(helper: MigrationTestHelper, startDbVersion: Int, endDbVersion: Int, migration: Migration, playerCount: Int = 2, block: (db: AppDatabase) -> Unit) {
            helper.createDatabase(databaseName, startDbVersion).apply {
                generateGameData(startDbVersion, this, playerCount)
                // Prepare for the next version.
                close()
            }

            // Re-open the database with the end version and provide the migration
            helper.runMigrationsAndValidate(databaseName, endDbVersion, true, migration)

            // MigrationTestHelper automatically verifies the schema changes,
            // but you need to validate that the data was migrated properly.
            val db = getAppDatabaseWithMigrations()
            block(db)
            db.close()

            // Clean the app database, allows the next test to create the db at the version that it needs
            AppDatabase.clean()
        }

        fun now(): String {
            return ZonedDateTime.now().format(GameSettings.DATE_FORMATTER)
        }

        private fun generateGameData(version: Int, db: SupportSQLiteDatabase, playerCount: Int) {
            val testN = 1
            if (version >= 4) {
                // Insert Games
                when {
                    version >= 8 -> insertGamesV8(testN, db)
                    version >= 7 -> insertGamesV7(testN, db)
                    version >= 6 -> insertGamesV6(testN, db)
                    else -> insertGamesV4(testN, db)
                }

                val players = testN * playerCount

                // Insert Users
                when {
                    version >= 10 -> insertUsersRawV10(players, db)
                    else -> insertUsersRawV4(players, db)
                }

                insertGamePlayersV4(players, db)
                insertScoresV4(version, players, db)
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

        private fun insertGamesV8(n: Int, db: SupportSQLiteDatabase) {
            val gameValues = List(n) { "($it, \"SimpleGame $it\", \"${now()}\", 0, 0, 0, 0, 0, 0, 0)" }
            val gameEntityColumnsV8 =
                "(${GameEntity.COLUMN_ID}, ${GameEntity.COLUMN_NAME}, ${GameEntity.COLUMN_LAST_PLAYED}, ${GameEntity.COLUMN_HAS_DEALER}, ${GameEntity.COLUMN_SHOW_ROUNDS}, ${GameEntity.COLUMN_REVERSED_SCORING}, ${GameEntity.COLUMN_MAX_SCORE}, ${GameEntity.COLUMN_MAX_ROUNDS}, ${GameEntity.COLUMN_SHOW_ROUND_NOTES}, ${GameEntity.COLUMN_USE_CALCULATOR})"

            db.execSQL("INSERT INTO ${GameEntity.TABLE_NAME} $gameEntityColumnsV8 VALUES ${gameValues.joinToString()}")
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
            insertUsersRawV10(n, db);
        }

        private fun insertUsersRawV4(n: Int, db: SupportSQLiteDatabase) {
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

        private fun insertUsersRawV10(n: Int, db: SupportSQLiteDatabase) {
            var userValues = ""
            repeat(n) {
                userValues = userValues.plus("($it, \"User $it\", 0)")
                if (it < n - 1) {
                    userValues = userValues.plus(", ")
                }
            }
            db.execSQL("INSERT INTO ${PlayerEntity.TABLE_NAME} " +
                "(${PlayerEntity.COLUMN_ID}, ${PlayerEntity.COLUMN_NAME}, ${PlayerEntity.COLUMN_ARCHIVED}) " +
                "VALUES $userValues")
        }
    }
}
