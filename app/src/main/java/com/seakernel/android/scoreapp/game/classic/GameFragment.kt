package com.seakernel.android.scoreapp.game.classic

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.calculator.CalculatorKeyboardView
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.databinding.FragmentGameBinding
import com.seakernel.android.scoreapp.game.DeleteRoundDialog
import com.seakernel.android.scoreapp.game.FinishGameDialog
import com.seakernel.android.scoreapp.game.PlayerRoundNotesDialog
import com.seakernel.android.scoreapp.game.PlayerStandingDialog
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.RoundRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setOptionalIconsVisible
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameFragment : MobiusFragment<GameModel, GameEvent, GameEffect>() {

    interface GameListener {
        fun onGameSettingsSelected(gameId: Long)
        fun onGraphSelected(gameId: Long)
        fun onNewGame(gameId: Long, initialDealerId: Long?)
        fun onGameHasNoPlayers(gameId: Long)
    }

    private var listener: GameListener? = null
    private var gameRepository: GameRepository? = null
    private var roundRepository: RoundRepository? = null
    private var eventConsumer: Consumer<GameEvent>? = null

    // The game settings saves already counted when the game last loaded (see GameRepository.gameSettingsSaves)
    @Volatile
    private var loadedSettingsSaves = -1L

    // The number of players the score table is sized for
    private var tablePlayerCount = 0

    // The window's keyboard mode before this screen hid the keyboard, restored when leaving
    private var originalSoftInputMode: Int? = null

    private var _binding: FragmentGameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    init {
        loop = createLoop(GameModel.Companion::update)
        controller = MobiusAndroid.controller(
            loop,
            GameModel.createDefault(),
            ::initMobius
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? GameListener
        gameRepository = GameRepository(requireContext())
        roundRepository = RoundRepository(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        gameRepository = null
        roundRepository = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.inflateMenu(R.menu.menu_game)
        binding.toolbar.setOptionalIconsVisible()

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsCutout()
        // Landscape puts the keypad beside the table instead of below it (see layout-land/fragment_game)
        val keypadBesideTable = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        binding.scoreScroll.applyWindowInsetsNavigationPadding(includeBottom = keypadBesideTable)
        binding.bottomContainer.applyWindowInsetsNavigationPadding(includeStart = !keypadBesideTable)
        if (keypadBesideTable) makeRoomForKeypad()
        syncHorizontalScroll(binding.nameScroll, binding.scoreScroll)
        // Screen readers stop on the totals row as one item, read from its description (see TotalsAdapter)
        ViewCompat.setScreenReaderFocusable(binding.totalsRow, true)
        binding.scoreScroll.addOnLayoutChangeListener { view, left, _, right, _, oldLeft, _, oldRight, _ ->
            // Fit the table to the new width (e.g. after rotating), after this layout pass finishes
            if (right - left != oldRight - oldLeft) view.post { applyTableWidth() }
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionEdit -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        listener?.onGameSettingsSelected(gameId)
                    }
                    true
                }
                R.id.actionGraph -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        listener?.onGraphSelected(gameId)
                    }
                    true
                }
                R.id.actionStanding -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        showStandingDialog(gameId)
                    }
                    true
                }
                R.id.actionDeleteRounds -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        showRoundDeleteDialog(gameId)
                    }
                    true
                }
                R.id.actionNewGame -> {
                    eventConsumer?.accept(GameEvent.RequestNewGame)
                    true
                }
                else -> false
            }
        }

        val resultListener = fun(key: String, _: Bundle) {
            when (key) {
                REQUEST_DELETE_ROUND -> {
                    eventConsumer?.accept(GameEvent.RequestLoad)
                }
                REQUEST_NEW_GAME -> {
                    eventConsumer?.accept(GameEvent.RequestNewGame)
                }
                REQUEST_CHART -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        listener?.onGraphSelected(gameId)
                    }
                }
                REQUEST_FINISH_GAME -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        showStandingDialog(gameId, celebrate = true)
                    }
                }
                REQUEST_CONTINUE_GAME -> {
                    eventConsumer?.accept(GameEvent.RequestCreateRound)
                }
            }
        }
        requests.forEach { request ->
            parentFragmentManager.clearFragmentResultListener(request)
            parentFragmentManager.setFragmentResultListener(
                request,
                viewLifecycleOwner,
                resultListener
            )
        }

        // Coming back from Game Settings shows this screen while its changes are still saving, so load the game again
        // once a save it missed finishes
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                GameRepository.gameSettingsSaves.collect { saves ->
                    if (loadedSettingsSaves in 0 until saves) eventConsumer?.accept(GameEvent.RequestLoad)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.toolbar.setNavigationOnClickListener(null)
        _binding = null
    }

    /** Sets up the player names and the totals row */
    private fun setupHeaderAndFooter(model: GameModel) {
        val settings = model.settings
        val players = settings.players
        updateTableWidth(players.size)

        if ((binding.totalsRow.layoutManager as? GridLayoutManager)?.spanCount != players.size) {
            binding.totalsRow.layoutManager = GridLayoutManager(requireContext(), players.size)
        }
        if ((binding.nameRow.layoutManager as? GridLayoutManager)?.spanCount != players.size) {
            binding.nameRow.layoutManager = GridLayoutManager(requireContext(), players.size)
        }

        val adapter = PlayersAdapter(
            settings.showRoundNotes,
            players,
            object :
                PlayerViewHolder.PlayerHolderClickedListener {
                override fun playerHolderClicked(player: Player) {
                    showRoundNotesDialog(player, settings.id!!)
                }
            })
        binding.nameRow.swapAdapter(adapter, false)
    }

    private fun updateTableWidth(playerCount: Int) {
        tablePlayerCount = playerCount
        applyTableWidth()
    }

    /**
     * For the keypad beside the table: shows its container only together with it (an empty one would still keep
     * clear of the screen edge), and moves the end of the names and scores to the keypad's side while it shows
     */
    private fun makeRoomForKeypad() {
        val container = binding.bottomContainer
        val edgeSpacing = resources.getDimensionPixelSize(R.dimen.score_table_edge_spacing)
        val fitBesideKeypad = fit@{
            val binding = _binding ?: return@fit
            // Without the container's padding, since the names and scores already keep clear of the screen edge
            val marginEnd = edgeSpacing + if (container.isVisible) binding.calculatorKeyboard.width else 0
            // Both move as far, so each name stays above its scores
            listOf(binding.nameScroll, binding.scoreScroll).forEach { scroll ->
                if (scroll.marginEnd != marginEnd) {
                    scroll.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        // Set again, since a margin set from layout_marginHorizontal is lost once the end one changes
                        marginStart = edgeSpacing
                        this.marginEnd = marginEnd
                    }
                }
            }
        }

        binding.calculatorKeyboard.onShownChanged = { shown ->
            container.isVisible = shown
            if (!shown) fitBesideKeypad() // A hidden container isn't laid out again
        }
        container.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            // After this layout pass finishes
            view.post(fitBesideKeypad)
        }
    }

    /**
     * Keeps every column wide enough to read and tap, scaling with the text size. When the players don't fit
     * across the screen, the names and scores scroll sideways instead of being squeezed.
     *
     * The rows get an exact width, since a horizontal scroll view lets its content measure as wide as it likes,
     * which would make the name and score columns different widths.
     */
    private fun applyTableWidth() {
        val binding = _binding ?: return
        val scroll = binding.scoreScroll
        val screenWidth = scroll.width - scroll.paddingLeft - scroll.paddingRight
        if (screenWidth <= 0 || tablePlayerCount == 0) return

        // Scaled by the font scale directly, since large sp values barely grow with nonlinear font scaling
        val minColumnWidth = MIN_COLUMN_WIDTH_DP * resources.displayMetrics.density * resources.configuration.fontScale
        val tableWidth = max(screenWidth, (tablePlayerCount * minColumnWidth).toInt())
        listOf(binding.nameRow, binding.scoreRows, binding.totalsRow).forEach { row ->
            if (row.layoutParams.width != tableWidth) {
                row.updateLayoutParams { width = tableWidth }
            }
        }
        centerAddRoundButton()
    }

    /**
     * How far the add round button moves from the middle of the table to be in the middle of the screen. Only a
     * table wider than the screen moves it, following the sideways scroll.
     */
    private fun addRoundButtonOffset(): Float {
        val binding = _binding ?: return 0f
        val scroll = binding.scoreScroll
        val screenWidth = scroll.width - scroll.paddingLeft - scroll.paddingRight
        val tableWidth = binding.scoreRows.layoutParams.width
        if (screenWidth <= 0 || tableWidth <= screenWidth) return 0f
        return scroll.scrollX + screenWidth / 2f - tableWidth / 2f
    }

    private fun centerAddRoundButton() {
        val rows = _binding?.scoreRows ?: return
        val lastPosition = (rows.adapter?.itemCount ?: return) - 1
        (rows.findViewHolderForAdapterPosition(lastPosition) as? AddRoundViewHolder)
            ?.setHorizontalOffset(addRoundButtonOffset())
    }

    /** Keeps the name row and the score table scrolled to the same players, with the add round button centered */
    private fun syncHorizontalScroll(first: View, second: View) {
        first.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (second.scrollX != scrollX) second.scrollTo(scrollX, 0)
        }
        second.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (first.scrollX != scrollX) first.scrollTo(scrollX, 0)
            centerAddRoundButton()
        }
    }

    private fun showStandingDialog(gameId: Long, celebrate: Boolean = false) {
        logEvent(AnalyticsConstants.Event.SHOW_PLAYER_STANDING_DIALOG)
        val dialog = PlayerStandingDialog.newInstance(gameId, celebrate)
        dialog.show(parentFragmentManager, PlayerStandingDialog::class.java.name)
    }

    private fun showRoundNotesDialog(player: Player, gameId: Long) {
        logEvent(AnalyticsConstants.Event.SHOW_ROUND_NOTES_DIALOG)
        val dialog = PlayerRoundNotesDialog.newInstance(player, gameId)
        dialog.show(parentFragmentManager, PlayerRoundNotesDialog::class.java.name)
    }

    private fun showRoundDeleteDialog(gameId: Long) {
        logEvent(AnalyticsConstants.Event.SHOW_ROUND_DELETE_DIALOG)
        val appContext = requireContext().applicationContext // Can't be read off the main thread
        lifecycleScope.launch(Dispatchers.IO) {
            val ids = RoundRepository(appContext).getRoundIds(gameId)
            withContext(Dispatchers.Main) {
                val dialog = DeleteRoundDialog.newInstance(ids)
                dialog.show(parentFragmentManager, DeleteRoundDialog::class.java.name)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.GameFragment)

        // Don't open the keyboard when coming back to a game. The window is shared with every other screen, so
        // only the keyboard state changes here (keeping how the screen resizes for it), and it's put back on pause.
        activity?.window?.let { window ->
            val mode = window.attributes.softInputMode
            originalSoftInputMode = mode
            window.setSoftInputMode(
                (mode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE.inv()) or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            )
        }
    }

    override fun onPause() {
        super.onPause()
        originalSoftInputMode?.let { activity?.window?.setSoftInputMode(it) }
        originalSoftInputMode = null
    }

    // Mobius functions

    override fun initMobius(model: GameModel): First<GameModel, GameEffect> {
        return First.first(model, setOf(GameEffect.FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<GameEvent>): Connection<GameModel> {
        this.eventConsumer = eventConsumer

        return object : Connection<GameModel> {
            private var lastModel: GameModel? = null
            private var lastLeadPlayerIds: List<Long> = emptyList()
            private var lastCelebratedPlayerId: Long? = null
            private var confettiJob: Job? = null
            private var celebrationJob: Job? = null
            private var pendingSingleLeadId: Long? = null

            // Whether the target score was reached when confetti was last checked, and the last round it was first
            // reached in, so reaching it celebrates once a round (not again for each edit to that round)
            private var wasGoalScoreReached = false
            private var goalScoreCelebratedRoundId: Long? = null

            override fun accept(model: GameModel) {
                if (model == lastModel) return
                val isFirstLoad = lastModel?.settings?.id == null

                if (model.settings != lastModel?.settings) {
                    binding.toolbar.title = model.settings.name
                    binding.calculatorKeyboard.isCalculatorEnabled = model.settings.useCalculator
                    // Opening the game, or changing its target, doesn't count as reaching it
                    wasGoalScoreReached = model.hasReachedGoalScore()
                }

                var manager = binding.scoreRows.layoutManager as? GridLayoutManager
                val oldSpanCount = manager?.spanCount
                if (model.settings.players.isNotEmpty()) {
                    if (model.settings != lastModel?.settings) {
                        setupHeaderAndFooter(model)
                    }

                    val spanCount = model.settings.players.size
                    if (oldSpanCount != spanCount) {
                        // Reset the layout manager if the number of players has changed
                        manager = GridLayoutManager(requireContext(), spanCount)
                        binding.scoreRows.layoutManager = manager
                        GameScoreAdapter.sizeRecycledViewPool(binding.scoreRows.recycledViewPool, spanCount)
                    }

                    if (oldSpanCount != spanCount || model.rounds.size != lastModel?.rounds?.size) {
                        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int) =
                                if (position == (model.rounds.size * spanCount)) spanCount else 1
                        }
                    }
                }

                // If count is not empty (account for empty with add round row), and new count is greater (added a row)
                val oldCount = binding.scoreRows.adapter?.itemCount ?: 0
                binding.scoreRows.swapAdapter(
                    GameScoreAdapter(
                        model.settings.hasDealer,
                        model.settings.useCalculator,
                        model.rounds,
                        model.focusedScoreId,
                        model.isGoalReached,
                        eventConsumer,
                        ::addRoundButtonOffset,
                    ) { score -> binding.calculatorKeyboard.setInput(score) }, false
                )
                val newCount = binding.scoreRows.adapter!!.itemCount

                if ((isFirstLoad && newCount > 0) || (oldCount in 2 until newCount && oldSpanCount == model.settings.players.size)) {
                    // When a new round is being inserted, or it's the first load, scroll to the bottom so that new rounds can be added (likely action when loading up the game again)
                    binding.scoreRows.post {
                        _binding?.let {
                            binding.scoreRows.scrollToPosition(newCount - 1)
                        }
                    }
                }

                val totalsAdapter = TotalsAdapter(
                    model.settings.players,
                    model.playerTotals,
                    model.leadPlayerIds,
                )
                binding.totalsRow.swapAdapter(totalsAdapter, false)
                binding.totalsRow.contentDescription = totalsAdapter.describe(requireContext())
                lastLeadPlayerIds = model.leadPlayerIds

                val currentSingleLeadId = if (lastLeadPlayerIds.size == 1) lastLeadPlayerIds[0] else null
                pendingSingleLeadId = currentSingleLeadId

                if (isFirstLoad) {
                    lastCelebratedPlayerId = currentSingleLeadId
                } else {
                    val focusLost = lastModel?.focusedScoreId != null && model.focusedScoreId == null
                    val scoreChanged = model.rounds != lastModel?.rounds

                    if (focusLost) {
                        confettiJob?.cancel()
                        // The keypad hides a moment after the score loses focus, moving the totals down, so the
                        // confetti waits for that to come from where the total ends up
                        triggerConfettiIfChanged(celebrateAfterMs = CalculatorKeyboardView.HIDE_DELAY_MS)
                    } else if (scoreChanged) {
                        confettiJob?.cancel()
                        confettiJob = lifecycleScope.launch {
                            delay(CONFETTI_DEBOUNCE_MS.milliseconds)
                            triggerConfettiIfChanged()
                        }
                    }
                }

                lastModel = model
            }

            /**
             * Once the target score is reached, celebrates the leader when someone first reaches it in a round, and
             * whenever someone else takes the lead
             */
            private fun triggerConfettiIfChanged(celebrateAfterMs: Long = 0) {
                val model = lastModel ?: return
                val goalReached = model.hasReachedGoalScore()
                val roundId = model.rounds.lastOrNull()?.id
                val firstReachedThisRound = goalReached && !wasGoalScoreReached && roundId != goalScoreCelebratedRoundId
                wasGoalScoreReached = goalReached

                val leadId = pendingSingleLeadId ?: return
                if (!goalReached || (leadId == lastCelebratedPlayerId && !firstReachedThisRound)) return

                lastCelebratedPlayerId = leadId
                if (firstReachedThisRound) goalScoreCelebratedRoundId = roundId

                celebrationJob?.cancel()
                celebrationJob = lifecycleScope.launch {
                    delay(celebrateAfterMs.milliseconds)
                    // Each model swaps in a new totals adapter, which has no view holders until it lays out, so wait
                    // for that (losing focus right after the score changes checks before the totals lay out again)
                    _binding?.totalsRow?.post {
                        val totalsRow = _binding?.totalsRow ?: return@post
                        val position = (totalsRow.adapter as? TotalsAdapter)?.getPlayerPosition(leadId) ?: -1
                        if (position != -1) {
                            val holder = totalsRow.findViewHolderForAdapterPosition(position) as? ScoreViewHolder
                            holder?.celebrate()
                        }
                    }
                }
            }

            override fun dispose() {
                confettiJob?.cancel()
                celebrationJob?.cancel()
            }
        }
    }

    override fun effectHandler(eventConsumer: Consumer<GameEvent>): Connection<GameEffect> {
        return object : Connection<GameEffect> {
            private fun getFirstRoundDealer(game: Game): Player? {
                if (game.settings.hasDealer) {
                    if (game.settings.initialDealerId != null) {
                        game.settings.players.firstOrNull { it.id == game.settings.initialDealerId }
                            ?.let { player ->
                                return player
                            }
                    }
                    return game.settings.players.takeIf { it.isNotEmpty() }?.random()
                }

                return null
            }

            private fun createFirstRound(game: Game) = Round(
                dealer = getFirstRoundDealer(game),
                number = 0,
                scores = game.settings.players.map { player ->
                    Score(player = player)
                }
            )

            override fun accept(effect: GameEffect) {
                when (effect) {
                    is GameEffect.FetchData -> {
                        val gameId = arguments?.getLong(ARG_GAME_ID, 0) ?: 0
                        val repository = gameRepository ?: return // The screen closed before the game loaded
                        loadedSettingsSaves = GameRepository.gameSettingsSaves.value
                        val game = repository.loadFullGame(gameId)
                        when {
                            // The game was deleted, so there's nothing to show
                            game == null -> runWhenStarted { requireActivity().onBackPressedDispatcher.onBackPressed() }

                            // Every player in the game was deleted, so it needs new players before it can be played
                            game.settings.players.isEmpty() -> runWhenStarted { listener?.onGameHasNoPlayers(gameId) }

                            // If we load a game that has no rounds, we need to fix it
                            game.rounds.isEmpty() ->
                                roundRepository?.addOrUpdateRound(gameId, createFirstRound(game))?.let { round ->
                                    eventConsumer.accept(GameEvent.Loaded(game.copy(rounds = listOf(round))))
                                }

                            else -> eventConsumer.accept(GameEvent.Loaded(game))
                        }
                    }

                    is GameEffect.SaveRound -> {
                        roundRepository?.addOrUpdateRound(effect.gameId, effect.round)?.let {
                            eventConsumer.accept(
                                GameEvent.RoundSaved(
                                    it
                                )
                            )
                        }
                    }

                    is GameEffect.SaveScore -> {
                        roundRepository?.updateScore(effect.score)?.let {
                            eventConsumer.accept(
                                GameEvent.ScoreSaved(
                                    effect.roundId,
                                    it
                                )
                            )
                        }
                    }

                    is GameEffect.NewGame -> runWhenStarted {
                        listener?.onNewGame(effect.gameId, effect.initialPlayerId)
                        logEvent(AnalyticsConstants.Event.NEW_GAME_CLICKED)
                    }

                    is GameEffect.ShowFinishGame -> runWhenStarted {
                        FinishGameDialog.newInstance()
                            .show(parentFragmentManager, FinishGameDialog::class.java.name)
                    }
                }.hashCode() // Exhaustive call
            }

            override fun dispose() {
                // TODO("not implemented")
            }
        }
    }

    // End Mobius functions

    companion object {
        private const val CONFETTI_DEBOUNCE_MS = 600L // 250 - 400 based on user typing/pausing
        private const val MIN_COLUMN_WIDTH_DP = 48f // A touch target wide at the default text size

        private const val ARG_GAME_ID = "game_id"

        const val REQUEST_DELETE_ROUND = "DELETE_ROUND"
        const val REQUEST_NEW_GAME = "NEW_GAME"
        const val REQUEST_CHART = "CHART"
        const val REQUEST_FINISH_GAME = "FINISH_GAME"
        const val REQUEST_CONTINUE_GAME = "CONTINUE_GAME"

        val requests = listOf(
            REQUEST_DELETE_ROUND,
            REQUEST_NEW_GAME,
            REQUEST_CHART,
            REQUEST_FINISH_GAME,
            REQUEST_CONTINUE_GAME,
        )

        fun newInstance(gameId: Long): GameFragment {
            val fragment = GameFragment()
            val args = Bundle().apply {
                putLong(ARG_GAME_ID, gameId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
