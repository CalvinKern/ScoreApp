package com.seakernel.android.scoreapp.gamecreate

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hadilq.liveevent.LiveEvent
import com.seakernel.android.scoreapp.data.SimpleGame
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.seakernel.android.scoreapp.utility.safePostValue
import kotlinx.coroutines.*
import java.util.*

class GameSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var gameRepository: GameRepository = GameRepository(application)
    private var playerRepository: PlayerRepository = PlayerRepository(application)
    private val gameSettings = MutableLiveData<SimpleGame>().apply { value = SimpleGame() }
    private val gameCreatedEvent = LiveEvent<Long>()

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun getGameSettings(): LiveData<SimpleGame> = gameSettings

    fun getGameCreatedEvent(): LiveData<Long> = gameCreatedEvent

    @MainThread
    fun updateGameName(name: String) {
        gameSettings.value = gameSettings.value!!.copy(name = name)
    }

    fun createGame() {
        val settings = gameSettings.value ?: SimpleGame()

        scope.launch {
            val gameId = gameRepository.createGame(settings) // TODO: Use dealerId
            gameCreatedEvent.safePostValue(gameId)
        }
    }

    /**
     * Do a diff of the old players with the given IDs and add any new ones to the end of the player list.
     * Also need to remove any players that were in the old list but not in the given IDs.
     */
    fun updateForNewPlayers(playerIds: List<Long>) {
        val settings = gameSettings.value ?: SimpleGame()

        scope.launch {
            val players = settings.players.filter { playerIds.contains(it.id) }
            val newPlayers = playerRepository.loadUsers(playerIds.filterNot { id -> players.any { player -> player.id == id} })
            gameSettings.safePostValue(settings.copy(players = players + newPlayers))
        }
    }

    fun movePlayer(fromPosition: Int, toPosition: Int) {
        gameSettings.value?.let { settings ->
            val players = settings.players.toMutableList()
            Collections.swap(players, fromPosition, toPosition)
            gameSettings.value = settings.copy(players = players)
        }
    }
}
