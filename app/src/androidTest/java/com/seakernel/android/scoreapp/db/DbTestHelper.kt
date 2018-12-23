package com.seakernel.android.scoreapp.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.seakernel.android.scoreapp.data.Game
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
@Suppress("ClassName")
class DbTestHelper {

    private lateinit var playerDao: PlayerDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
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

        private fun now(): String {
            return ZonedDateTime.now().format(Game.DATE_FORMATTER)
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

        fun insertGamesRaw(n: Int, db: SupportSQLiteDatabase) {
            var gameValues = ""
            repeat(n) {
                gameValues = gameValues.plus("($it, Game $it, ${now()})")
                if (it < n - 1) {
                    gameValues = gameValues.plus(", ")
                }
            }

            db.execSQL("INSERT INTO ${GameEntity.TABLE_NAME} " +
                    "(${GameEntity.COLUMN_ID}, ${GameEntity.COLUMN_NAME}, ${GameEntity.COLUMN_LAST_PLAYED}) " +
                    "VALUES $gameValues")
        }

        fun generateDataRaw(n: Int, db: SupportSQLiteDatabase) {
            insertUsersRaw(n, db)
            insertGamesRaw(n, db)

            var gamePlayers = ""
            repeat(n) {
                gamePlayers = gamePlayers.plus("($it, $it)")
                if (it < n - 1) {
                    gamePlayers = gamePlayers.plus(", ")
                }
            }
            db.execSQL("INSERT INTO ${GamePlayerJoin.TABLE_NAME} " +
                    "(${GamePlayerJoin.COLUMN_GAME_ID}, ${GamePlayerJoin.COLUMN_PLAYER_ID}) " +
                    "VALUES $gamePlayers")
        }
    }
}
