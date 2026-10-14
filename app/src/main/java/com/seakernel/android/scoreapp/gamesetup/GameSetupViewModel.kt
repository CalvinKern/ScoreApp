package com.seakernel.android.scoreapp.gamesetup

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.hadilq.liveevent.LiveEvent
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.seakernel.android.scoreapp.utility.AppPreferences
import com.seakernel.android.scoreapp.utility.safePostValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime
import java.util.Collections

/**
 * @param savedState keeps a new game being set up when Android closes the app in the background (see [NewGameSetup]).
 * Existing games don't need it, they save to the database when the screen stops.
 */
class GameSetupViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var gameRepository: GameRepository = GameRepository(application)
    private var playerRepository: PlayerRepository = PlayerRepository(application)
    private val gameSettings = MutableLiveData<GameSettings?>().apply { value = null }
    private val gameCreatedEvent = LiveEvent<Long>()
    private val gameUpdatedEvent = LiveEvent<GameUpdated>()

    // What's saved for an existing game, to tell whether anything changed
    private var savedSettings: GameSettings? = null

    // Saves run one at a time in order, so an older save can't finish after a newer one
    private val saveDispatcher = Dispatchers.IO.limitedParallelism(1)

    // Existing games also save shortly after each change, in case the app is closed without the screen stopping
    private var autoSaveJob: Job? = null
    private val autoSaveObserver = Observer<GameSettings?> { settings ->
        if (savedSettings != null && settings != savedSettings) scheduleAutoSave()
        updateSaveStatus()
    }

    // Tells the screen an existing game saves on its own. Set up before init, since the observer above runs right away
    private val saveStatus = MutableLiveData<SaveStatus?>().apply { value = null }
    private var runningSaves = 0

    // Picks are applied one at a time in order, so an older pick can't finish loading after a newer one
    private val playersLock = Mutex()

    // A new game is kept in the saved state as it changes, to bring it back after Android closes the app
    private val newGameObserver = Observer<GameSettings?> { settings ->
        if (settings != null && settings.id == null) savedState[KEY_NEW_GAME] = NewGameSetup.of(settings)
    }

    // A new game coming back from the saved state, which takes the place of starting one (see initializeGame and
    // createCopy). Picks wait for it, so they apply to the game that came back.
    private val restoredNewGame: NewGameSetup? = savedState[KEY_NEW_GAME]
    private val newGameRestored = CompletableDeferred<Unit>()

    init {
        gameSettings.observeForever(autoSaveObserver)
        gameSettings.observeForever(newGameObserver)
        restoreNewGame()
    }
    private val gameNamesAutocomplete = MutableLiveData<List<String>?>().apply { value = null }

    private val saveProcessing = MutableLiveData<Boolean>().apply { value = false }

    override fun onCleared() {
        super.onCleared()
        gameSettings.removeObserver(autoSaveObserver)
        gameSettings.removeObserver(newGameObserver)
        job.cancel()
    }

    /** Brings back a new game from the saved state, with its players as they are now */
    private fun restoreNewGame() {
        val setup = restoredNewGame
        if (setup == null) {
            newGameRestored.complete(Unit)
            return
        }
        scope.launch {
            val players = playerRepository.loadActiveUsers(setup.playerIds)
            withContext(Dispatchers.Main) {
                gameSettings.value = setup.toSettings(players)
                newGameRestored.complete(Unit)
            }
        }
    }

    fun isSaving(): LiveData<Boolean> = saveProcessing

    /** Whether an existing game's changes are saved, or null for a new game (which is created with save) */
    fun getSaveStatus(): LiveData<SaveStatus?> = saveStatus

    fun getGameSettings(): LiveData<GameSettings?> = gameSettings

    fun getGameCreatedEvent(): LiveData<Long> = gameCreatedEvent

    fun getGameUpdatedEvent(): LiveData<GameUpdated> = gameUpdatedEvent

    fun getGameNamesForAutocomplete(): LiveData<List<String>?> = gameNamesAutocomplete

    @MainThread
    fun updateGameName(name: String) {
        val settings = gameSettings.value ?: return // Not loaded yet, its name shows once it is
        gameSettings.value = settings.copy(name = name)
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

    @MainThread
    fun setMaxScore(maxScore: Double?) {
        // A limit of 0 would end the game before it starts, so treat it as no limit
        gameSettings.value = (gameSettings.value ?: return).copy(maxScore = maxScore?.takeIf { it > 0 })
    }

    @MainThread
    fun setMaxRounds(maxRounds: Int?) {
        gameSettings.value = (gameSettings.value ?: return).copy(maxRounds = maxRounds?.takeIf { it > 0 })
    }

    /** Creates the new game being set up, existing games save their changes with [saveChanges] instead */
    fun saveGame() {
        if (saveProcessing.value == true) return

        saveProcessing.value = true
        val settings = gameSettings.value ?: GameSettings()

        scope.launch {
            val gameId = gameRepository.createGame(settings)
            gameCreatedEvent.safePostValue(gameId)
            withContext(Dispatchers.Main) {
                saveProcessing.value = false
            }
        }
    }

    /**
     * Saves any changes to an existing game, which saves automatically instead of with a save button.
     *
     * A blank name or no players can't be saved, so those keep what was saved before: the old name, and the old
     * players (saving no players would take everyone out of the game).
     *
     * @param exit whether the screen is closing, it's told when the save finishes (even if nothing changed)
     */
    @MainThread
    fun saveChanges(exit: Boolean) {
        autoSaveJob?.cancel()
        autoSaveJob = null

        val saved = savedSettings
        val current = gameSettings.value
        if (saved == null || current == null) {
            // Still loading, so nothing could have changed
            if (exit) gameUpdatedEvent.value = GameUpdated(saved?.id ?: current?.id ?: 0, exit = true, hasPlayers = false)
            updateSaveStatus()
            return
        }

        val settings = current.copy(
            name = current.name.takeIf { it.isNotBlank() } ?: saved.name,
            players = current.players.takeIf { it.isNotEmpty() } ?: saved.players,
        )
        val update = GameUpdated(settings.id!!, exit, hasPlayers = settings.players.isNotEmpty())
        if (settings == saved) {
            // Wait for any save still running, so the next screen loads what was saved
            if (exit) scope.launch(saveDispatcher) { gameUpdatedEvent.safePostValue(update) }
            updateSaveStatus()
            return
        }

        savedSettings = settings
        runningSaves++
        updateSaveStatus()
        scope.launch(saveDispatcher) {
            gameRepository.updateGame(settings)
            gameUpdatedEvent.safePostValue(update)
            withContext(Dispatchers.Main) {
                runningSaves--
                updateSaveStatus()
            }
        }
    }

    @MainThread
    private fun updateSaveStatus() {
        val saved = savedSettings
        val current = gameSettings.value
        saveStatus.value = when {
            saved == null || current == null -> null
            // These keep what was saved before (see saveChanges)
            current.name.isBlank() -> SaveStatus.NAME_NOT_SAVED
            current.players.isEmpty() && saved.players.isNotEmpty() -> SaveStatus.PLAYERS_NOT_SAVED
            // Every player was deleted, which is why the game opened here instead
            current.players.isEmpty() -> SaveStatus.PLAYERS_NEEDED
            autoSaveJob != null || runningSaves > 0 -> SaveStatus.SAVING
            else -> SaveStatus.SAVED
        }
    }

    @MainThread
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch(Dispatchers.Main) {
            delay(AUTO_SAVE_DELAY_MS)
            autoSaveJob = null
            saveChanges(exit = false)
        }
    }

    /**
     * Do a diff of the old players with the given IDs and add any new ones to the end of the player list.
     * Also need to remove any players that were in the old list but not in the given IDs.
     */
    fun updateForNewPlayers(playerIds: List<Long>) {
        scope.launch {
            playersLock.withLock {
                newGameRestored.await()
                val newPlayers = playerRepository.loadUsers(playerIds)
                withContext(Dispatchers.Main) {
                    // Picks arrive while the picker is open, so they apply to the settings as they are once loaded
                    val settings = gameSettings.value ?: GameSettings()
                    val dealerId =
                        if (settings.initialDealerId in playerIds) settings.initialDealerId else playerIds.firstOrNull()
                    gameSettings.value = settings.copy(
                        players = newPlayers,
                        initialDealerId = dealerId
                    )
                }
            }
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
                withContext(Dispatchers.Main) {
                    savedSettings = it
                    gameSettings.value = it
                }
            }
        }
    }

    fun createCopy(gameId: Long, initialDealerId: Long?) {
        loadGameNames()

        if (gameSettings.value != null || restoredNewGame != null) return // Opened already, or coming back

        scope.launch {
            gameRepository.loadGame(gameId)?.let {
                gameSettings.safePostValue(
                    it.copy(
                        id = null,
                        lastPlayed = ZonedDateTime.now(),
                        initialDealerId = initialDealerId,
                    )
                )
            }
        }
    }

    /**
     * Load the settings and players from the most recently played game with the given name into the
     * new game being set up. Games that already exist are never changed, and neither is a setup
     * that already has players, so nothing the user picked gets overwritten.
     */
    fun loadPreviousSettings(name: String) {
        if (!canLoadPreviousSettings()) return

        scope.launch {
            val previous = gameRepository.loadMostRecentGame(name) ?: return@launch
            withContext(Dispatchers.Main) {
                val current = gameSettings.value ?: GameSettings()
                // Re-check in case the game was saved or players were added while loading
                if (!canLoadPreviousSettings(current)) return@withContext

                gameSettings.value = previous.copy(
                    id = null,
                    name = current.name,
                    lastPlayed = ZonedDateTime.now(),
                )
            }
        }
    }

    /** Previous settings only load into a new game that has no players yet */
    fun canLoadPreviousSettings(settings: GameSettings? = gameSettings.value) =
        settings?.id == null && settings?.players.isNullOrEmpty()

    @MainThread
    fun initializeGame() {
        if (gameSettings.value == null && restoredNewGame == null) { // Not opened already, or coming back
            gameSettings.value = GameSettings(useCalculator = AppPreferences.isCalculatorDefaultEnabled(getApplication()))
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

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 1_500L
        private const val KEY_NEW_GAME = "new_game"
    }
}

/**
 * An existing game's changes were saved.
 *
 * @param exit whether the screen asked to close once saved
 * @param hasPlayers whether the game can be played
 */
data class GameUpdated(val gameId: Long, val exit: Boolean, val hasPlayers: Boolean)

/** Whether an existing game's changes are saved, shown under the title of its settings */
enum class SaveStatus {
    /** A change is waiting to save, or saving */
    SAVING,
    SAVED,
    /** The name is blank, so the old name is kept */
    NAME_NOT_SAVED,
    /** No players are picked, so the old players are kept */
    PLAYERS_NOT_SAVED,
    /** The game has no players, so it can't be played until some are picked */
    PLAYERS_NEEDED,
}
