package com.seakernel.android.scoreapp.gamecreate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.seakernel.android.scoreapp.data.SimpleGame
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import kotlinx.coroutines.*

class GameSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var gameRepository: GameRepository = GameRepository(application)
    private var playerRepository: PlayerRepository = PlayerRepository(application)
    private val gameSettings = MutableLiveData<SimpleGame>().apply { value = SimpleGame() }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun getGameSettings(): LiveData<SimpleGame> {
        return gameSettings
    }

    /**
     * Do a diff of the old players with the given IDs and add any new ones to the end of the player list.
     * Also need to remove any players that were in the old list but not in the given IDs.
     */
    fun updateForNewPlayers(playerIds: List<Long>) {
        scope.launch {
            val oldPlayerIds = gameSettings.value?.players?.map { it.id } ?: emptyList()
            val removedPlayers = oldPlayerIds.filterNot { playerIds.contains(it) }
            val players = playerRepository.loadUsers(playerIds.filterNot { oldPlayerIds.contains(it) })
            withContext(Dispatchers.Main) {
                gameSettings.value = gameSettings.value?.copy(
                    players = gameSettings.value!!.players.filterNot { removedPlayers.contains(it.id) } + players
                )
            }
        }
    }
}
