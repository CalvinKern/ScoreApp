package com.seakernel.android.scoreapp.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.utility.safePostValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private val gameData = MutableLiveData<Game>()

    private var gameRepository: GameRepository = GameRepository(application)

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun getGame(): LiveData<Game> = gameData

    fun loadGame(gameId: Long, forceRefresh: Boolean = false) {
        // Return early if it is the same game
        if (gameData.value?.settings?.id == gameId && !forceRefresh) return

        scope.launch {
            gameRepository.loadFullGame(gameId).let {
                gameData.safePostValue(it)
            }
        }
    }
}