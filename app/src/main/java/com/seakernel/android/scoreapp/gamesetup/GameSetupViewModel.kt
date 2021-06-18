package com.seakernel.android.scoreapp.gamesetup

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hadilq.liveevent.LiveEvent
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.seakernel.android.scoreapp.utility.safePostValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class GameSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var gameRepository: GameRepository = GameRepository(application)
    private var playerRepository: PlayerRepository = PlayerRepository(application)
    private val gameSettings = MutableLiveData<GameSettings?>().apply { value = null }
    private val gameCreatedEvent = LiveEvent<Long>()
    private val gameUpdatedEvent = LiveEvent<Long>()
    private val gameNamesAutocomplete = MutableLiveData<List<String>?>().apply { value = null }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun getGameSettings(): LiveData<GameSettings?> = gameSettings

    fun getGameCreatedEvent(): LiveData<Long> = gameCreatedEvent

    fun getGameUpdatedEvent(): LiveData<Long> = gameUpdatedEvent

    fun getGameNamesForAutocomplete(): LiveData<List<String>?> = gameNamesAutocomplete

    @MainThread
    fun updateGameName(name: String) {
        gameSettings.value = gameSettings.value!!.copy(name = name)
    }

    @MainThread
    fun setHasDealer(hasDealer: Boolean) {
        gameSettings.value = gameSettings.value!!.copy(hasDealer = hasDealer)
    }

    @MainThread
    fun setReverseScoring(reverseScoring: Boolean) {
        gameSettings.value = gameSettings.value!!.copy(reversedScoring = reverseScoring)
    }

    @MainThread
    fun setShowNotes(showNotes: Boolean) {
        gameSettings.value = gameSettings.value!!.copy(showRoundNotes = showNotes)
    }

    @MainThread
    fun setUseCalculator(useCalculator: Boolean) {
        gameSettings.value = gameSettings.value!!.copy(useCalculator = useCalculator)
    }

    fun saveGame() {
        val settings = gameSettings.value ?: GameSettings()

        scope.launch {
            if (settings.id != null) {
                gameRepository.updateGame(settings)
                gameUpdatedEvent.safePostValue(settings.id)
            } else {
                val gameId = gameRepository.createGame(settings)
                gameCreatedEvent.safePostValue(gameId)
            }
        }
    }

    /**
     * Do a diff of the old players with the given IDs and add any new ones to the end of the player list.
     * Also need to remove any players that were in the old list but not in the given IDs.
     */
    fun updateForNewPlayers(playerIds: List<Long>) {
        val settings = gameSettings.value ?: GameSettings()

        scope.launch {
            val dealerId = if (settings.initialDealerId in playerIds) settings.initialDealerId else playerIds.firstOrNull()
            val newPlayers = playerRepository.loadUsers(playerIds)
            gameSettings.safePostValue(settings.copy(players = newPlayers, initialDealerId = dealerId))
        }
    }

    fun movePlayer(fromPosition: Int, toPosition: Int) {
        gameSettings.value?.let { settings ->
            val players = settings.players.toMutableList()
            Collections.swap(players, fromPosition, toPosition)
            gameSettings.value = settings.copy(players = players)
        }
    }

    fun setDealer(playerId: Long) {
        gameSettings.value?.let { settings ->
            gameSettings.value = settings.copy(initialDealerId = playerId)
        }
    }

    fun loadGame(gameId: Long, forceRefresh: Boolean = false) {
        loadGameNames()

        // Return early if it is the same game
        if (gameSettings.value?.id == gameId && !forceRefresh) return

        scope.launch {
            gameRepository.loadGame(gameId)?.let {
                gameSettings.safePostValue(it)
            }
        }
    }

    @MainThread
    fun initializeGame() {
        if (gameSettings.value == null) {
            gameSettings.value = GameSettings()
        }
        loadGameNames()
    }

    private fun loadGameNames() {
        scope.launch {
            gameRepository.gameNameSearch().let {
                gameNamesAutocomplete.safePostValue(it)
            }
        }
    }
}
