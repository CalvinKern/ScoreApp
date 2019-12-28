package com.seakernel.android.scoreapp.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.seakernel.android.scoreapp.database.AppDatabase
import com.seakernel.android.scoreapp.database.daos.PlayerDao
import com.seakernel.android.scoreapp.database.entities.PlayerEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("ClassName")
class PlayerDaoTest {

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
    fun writePlayerAndReadAll() {
        val player =
            PlayerEntity(
                0,
                "User 1"
            )
        val playerIds = playerDao.insertAll(player)
        val allPlayers = playerDao.getAll()

        assertEquals(allPlayers[0].uid, playerIds[0])
        assertEquals(allPlayers[0].name, "User 1")
    }

    @Test
    fun writePlayerAndFindId() {
        val player =
            PlayerEntity(
                0,
                "User 1"
            )
        val playerIds = playerDao.insertAll(player)
        val playerList = playerDao.loadAllByIds(longArrayOf(playerIds[0]))

        assertNotNull(playerList[0])
    }

    @Test
    fun updatePlayer() {
        val player =
            PlayerEntity(
                0,
                "User 1"
            )
        val playerIds = playerDao.insertAll(player)

        playerDao.update(
            PlayerEntity(
                playerIds[0],
                "User one"
            )
        )

        val playerList = playerDao.loadAllByIds(longArrayOf(playerIds[0]))
        assertEquals(playerList[0].name, "User one")
    }

    @Test
    fun deletePlayer() {
        val player =
            PlayerEntity(
                0,
                "User 1"
            )
        val playerIds = playerDao.insertAll(player)

        playerDao.deleteById(playerIds[0])

        val allPlayers = playerDao.getAll()
        assertEquals(0, allPlayers.size)
    }

    @Test
    fun insertMultiplePlayers() {
        val n = 5
        val players = mutableListOf<PlayerEntity>()
        repeat(n) { players.add(
            PlayerEntity(
                0,
                "User $it"
            )
        ) }

        playerDao.insertAll(*players.toTypedArray())

        val allPlayers = playerDao.getAll()
        assertEquals(n, allPlayers.size)
    }
}
