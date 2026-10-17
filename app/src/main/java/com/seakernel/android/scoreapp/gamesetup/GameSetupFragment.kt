package com.seakernel.android.scoreapp.gamesetup

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.calculator.ScoreKeyListener
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.FragmentGameCreateBinding
import com.seakernel.android.scoreapp.databinding.HolderGameCreatePlayerBinding
import com.seakernel.android.scoreapp.databinding.ViewGameSettingsBinding
import com.seakernel.android.scoreapp.playerselect.PlayerSelectFragment
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.AppPreferences
import com.seakernel.android.scoreapp.utility.ScoreFormat
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.isCheckedSafe
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setBackgroundRipple
import com.seakernel.android.scoreapp.utility.setOptionalIconsVisible
import com.seakernel.android.scoreapp.utility.setStatusSubtitle
import com.seakernel.android.scoreapp.utility.setVisible
import com.seakernel.android.scoreapp.utility.themeColor

class GameSetupFragment : Fragment() {

    interface GameSetupListener {
        /** @param savesAutomatically whether the picks save to the game as they're made (for an existing game) */
        fun onShowPlayerSelectScreen(playerIds: List<Long>, savesAutomatically: Boolean)
        fun onShowGameScreen(gameId: Long)
        fun onGameUpdated()
    }

    private var listener: GameSetupListener? = null
    private var nameTextWatcher: TextWatcher? = null
    private var maxScoreTextWatcher: TextWatcher? = null
    private var maxRoundsTextWatcher: TextWatcher? = null

    private val gameUpdatedObserver = Observer<GameUpdated> { update ->
        if (!update.exit) return@Observer // Saved while the screen stays open

        if (isOpenGameOnSave && update.hasPlayers) {
            listener?.onShowGameScreen(update.gameId)
        } else {
            listener?.onGameUpdated()
        }
    }

    /**
     * A game that needed players opens once it's saved, so back saves first and then opens it (see
     * gameUpdatedObserver). Other existing games save as the screen stops (see onStop), so back leaves right away and
     * the game slides back in under a back gesture.
     */
    private val saveOnBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            isEnabled = false // Leave once, even if back is pressed again while saving
            viewModel.saveChanges(exit = true)
        }
    }

    private val isExistingGame get() = arguments?.containsKey(ARG_GAME_ID) == true
    private val isOpenGameOnSave get() = arguments?.getBoolean(ARG_OPEN_GAME_ON_SAVE) == true
    private val gameCreatedObserver =
        Observer<Long> { gameId -> listener?.onShowGameScreen(gameId) }
    private val saveObserver =
        Observer<Boolean?> { saving -> renderSettings(viewModel.getGameSettings().value, saving) }
    private val modelObserver =
        Observer<GameSettings?> { settings -> renderSettings(settings, viewModel.isSaving().value) }
    private val saveStatusObserver = Observer<SaveStatus?> { status ->
        val text = when (status) {
            SaveStatus.SAVING -> R.string.saveStatusSaving
            SaveStatus.SAVED -> R.string.saveStatusSaved
            SaveStatus.NAME_NOT_SAVED -> R.string.saveStatusNameNotSaved
            SaveStatus.PLAYERS_NOT_SAVED -> R.string.saveStatusPlayersNotSaved
            SaveStatus.PLAYERS_NEEDED -> R.string.gamePlayersRequired
            null -> null
        }
        // Only where it ends up is announced, not every time a change starts saving
        binding.toolbar.setStatusSubtitle(text?.let { getString(it) }, announce = status != SaveStatus.SAVING)
    }
    private val autocompleteObserver = Observer<List<String>?> { names ->
        binding.gameNameEdit.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names?.map { it.trim() }?.distinct() ?: emptyList(),
            )
        )
    }

    private val viewModel: GameSetupViewModel by viewModels<GameSetupViewModel>()
    private var _binding: FragmentGameCreateBinding? = null
    private var _settingsBinding: ViewGameSettingsBinding? = null

    private var hasFocusedGameName = false

    // Text shown from the game, which the text watchers ignore so it isn't saved back as typed
    private var isRenderingText = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as? GameSetupListener)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Picks arrive as they're made, while the picker covers this screen (so it's the activity that has to be
        // showing), and an existing game saves them right away. A later setup screen replaces this listener.
        val viewModel = viewModel
        parentFragmentManager.setFragmentResultListener(PlayerSelectFragment.REQUEST_PLAYERS, requireActivity()) { _, result ->
            viewModel.updateForNewPlayers(result.getLongArray(PlayerSelectFragment.KEY_PLAYER_IDS)?.toList().orEmpty())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameCreateBinding.inflate(layoutInflater, container, false)
        _settingsBinding = ViewGameSettingsBinding.bind(binding.root)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding?.playersHeaderEdit?.setOnClickListener(null)
        _settingsBinding?.hasDealerCheckbox?.setOnCheckedChangeListener(null)
        _settingsBinding?.hasDealerContainer?.setOnClickListener(null)
        _settingsBinding?.reversedScoringCheckbox?.setOnCheckedChangeListener(null)
        _settingsBinding?.reversedScoringContainer?.setOnClickListener(null)
        _settingsBinding?.showNotesCheckbox?.setOnCheckedChangeListener(null)
        _settingsBinding?.showNotesContainer?.setOnClickListener(null)
        _settingsBinding?.useCalculatorCheckbox?.setOnCheckedChangeListener(null)
        _settingsBinding?.useCalculatorContainer?.setOnClickListener(null)
        binding.gamePlayersEmptyImage.setOnClickListener(null)
        _binding?.gameNameEdit?.onItemClickListener = null
        _binding?.gameNameEdit?.setOnEditorActionListener(null)

        _binding?.gameNameEdit?.removeTextChangedListener(nameTextWatcher)
        _settingsBinding?.maxScoreEdit?.removeTextChangedListener(maxScoreTextWatcher)
        _settingsBinding?.maxRoundsEdit?.removeTextChangedListener(maxRoundsTextWatcher)
        _settingsBinding?.maxScoreEdit?.onFocusChangeListener = null
        _settingsBinding?.maxRoundsEdit?.onFocusChangeListener = null

        viewModel.getGameSettings().removeObserver(modelObserver)
        viewModel.getGameCreatedEvent().removeObserver(gameCreatedObserver)
        viewModel.getGameUpdatedEvent().removeObserver(gameUpdatedObserver)
        viewModel.getGameNamesForAutocomplete().removeObserver(autocompleteObserver)
        viewModel.getSaveStatus().removeObserver(saveStatusObserver)

        _binding = null
        _settingsBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup toolbar
        initToolbar()
        initSettings()

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsCutout()
        binding.gameEditContainer.applyWindowInsetsNavigationPadding(includeBottom = false)
        binding.playerRecycler.applyWindowInsetsNavigationPadding()

        // Start observing the data
        viewModel.getGameSettings().observe(viewLifecycleOwner, modelObserver)
        viewModel.getGameCreatedEvent().observe(viewLifecycleOwner, gameCreatedObserver)
        viewModel.getGameUpdatedEvent().observe(viewLifecycleOwner, gameUpdatedObserver)
        viewModel.getGameNamesForAutocomplete().observe(viewLifecycleOwner, autocompleteObserver)
        viewModel.getSaveStatus().observe(viewLifecycleOwner, saveStatusObserver)

        if (isExistingGame && isOpenGameOnSave) {
            saveOnBackCallback.isEnabled = true // Turned off after leaving once, and the view is recreated after picking players
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, saveOnBackCallback)
        }

        // Check for copy first, then opening a game
        if (arguments?.containsKey(ARG_GAME_COPY) == true) {
            val args = requireArguments()
            viewModel.createCopy(
                args.getLong(ARG_GAME_COPY),
                // Left out when there's no dealer, which getLong would read as player 0
                args.getLong(ARG_GAME_INITIAL_DEALER_ID).takeIf { args.containsKey(ARG_GAME_INITIAL_DEALER_ID) },
            )
        } else if (arguments?.containsKey(ARG_GAME_ID) == true) {
            viewModel.loadGame(requireArguments().getLong(ARG_GAME_ID))
        } else {
            viewModel.initializeGame()
        }
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.GameSetupFragment)
    }

    override fun onStop() {
        super.onStop()
        // Save when leaving with back, and when the app goes to the background (or another screen opens), in case
        // it's closed from there
        if (isExistingGame && !requireActivity().isChangingConfigurations) {
            viewModel.saveChanges(exit = false)
        }
    }

    private fun initToolbar() {
        // Leaving an existing game saves its changes (see onStop and saveOnBackCallback), leaving a new game discards it
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.setTitle(if (isExistingGame) R.string.gameSettingsTitle else R.string.gameCreateTitle)
        binding.toolbar.inflateMenu(R.menu.menu_game_create)
        binding.toolbar.setOptionalIconsVisible()
        // New games are created with save. Existing games save on their own, but save also saves and goes back
        // (or opens the game, for a game that needed players before it could be played).
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSave -> {
                    if (viewModel.getGameSettings().value?.name.isNullOrBlank()) {
                        context?.let {
                            Toast.makeText(it, R.string.gameNameRequired, Toast.LENGTH_SHORT).show()
                        }
                        return@setOnMenuItemClickListener false
                    } else if (viewModel.getGameSettings().value?.players.isNullOrEmpty()) {
                        context?.let {
                            Toast.makeText(it, R.string.gamePlayersRequired, Toast.LENGTH_SHORT).show()
                        }
                        return@setOnMenuItemClickListener false
                    }
                    if (isExistingGame) viewModel.saveChanges(exit = true) else viewModel.saveGame()
                    true
                }

                else -> false
            }
        }
    }

    private fun initSettings() {
        initListeners()

        // Setup player recycler
        binding.playersHeaderEdit.setOnClickListener {
            val ids =
                viewModel.getGameSettings().value?.players?.mapNotNull { it.id } ?: emptyList()
            listener?.onShowPlayerSelectScreen(ids, savesAutomatically = isExistingGame)
        }
        binding.gamePlayersEmptyImage.setOnClickListener {
            binding.playersHeaderEdit.performClick()
        }

        val adapter = PlayersAdapter(object : PlayerAdapterCallback {
            override fun onSelectedDealer(playerId: Long) {
                viewModel.setDealer(playerId)
            }
        })
        binding.playerRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.playerRecycler.adapter = adapter
        ItemTouchHelper(createItemTouchHelperCallback()).attachToRecyclerView(binding.playerRecycler)
    }

    private fun initListeners() {
        nameTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                if (isRenderingText) return
                viewModel.updateGameName(text.toString())
            }
        }

        maxScoreTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                if (isRenderingText) return
                viewModel.setMaxScore(ScoreFormat.parse(text.toString()))
            }
        }
        maxRoundsTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                if (isRenderingText) return
                viewModel.setMaxRounds(text.toString().toIntOrNull())
            }
        }

        // Typing updates the game from here on, and the game's text is shown with renderText
        binding.gameNameEdit.addTextChangedListener(nameTextWatcher)
        _settingsBinding?.maxScoreEdit?.addTextChangedListener(maxScoreTextWatcher)
        _settingsBinding?.maxRoundsEdit?.addTextChangedListener(maxRoundsTextWatcher)

        // Picking a previous game name for a new game loads that game's settings and players, as long
        // as no players have been added yet
        binding.gameNameEdit.setOnItemClickListener { parent, _, position, _ ->
            if (!viewModel.canLoadPreviousSettings()) return@setOnItemClickListener
            if (!AppPreferences.isAutoLoadGameSettingsEnabled(requireContext())) return@setOnItemClickListener

            val name = parent.getItemAtPosition(position) as? String ?: return@setOnItemClickListener
            logEvent(AnalyticsConstants.Event.GAME_SETTINGS_AUTO_LOADED)
            viewModel.loadPreviousSettings(name)
        }
        // The keyboard's action (done, next, ...) finishes the name, so the suggestions shouldn't stay open over the
        // settings. Not handled, so the action still runs.
        binding.gameNameEdit.setOnEditorActionListener { _, _, _ ->
            binding.gameNameEdit.dismissDropDown()
            false
        }

        val settingsBinding = _settingsBinding ?: return

        // The text isn't updated while a target field is being edited, so show what was actually
        // saved once it loses focus (e.g. a typed "0" is saved as no limit, which clears the field)
        val targetFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) viewModel.getGameSettings().value?.let { renderTargetFields(it) }
        }
        settingsBinding.maxScoreEdit.onFocusChangeListener = targetFocusListener
        settingsBinding.maxRoundsEdit.onFocusChangeListener = targetFocusListener
        // A target score is typed like a score, with decimals and a sign (a negative target is reached going down)
        settingsBinding.maxScoreEdit.keyListener = ScoreKeyListener()

        setListenerRow(settingsBinding.hasDealerContainer, settingsBinding.hasDealerCheckbox)
        setListenerRow(
            settingsBinding.reversedScoringContainer,
            settingsBinding.reversedScoringCheckbox
        )
        setListenerRow(settingsBinding.showNotesContainer, settingsBinding.showNotesCheckbox)
        setListenerRow(
            settingsBinding.useCalculatorContainer,
            settingsBinding.useCalculatorCheckbox
        )
    }

    private fun setListenerRow(container: View, checkbox: CheckBox) {
        container.setOnClickListener { checkbox.performClick() }
    }

    private fun renderSettings(settings: GameSettings?, isSaving: Boolean?) {
        if (settings == null) {
            return // TODO: Show loading spinner
        }
        binding.toolbar.menu.findItem(R.id.actionSave).isEnabled = isSaving != true
        binding.playerRecycler.setVisible(settings.players.isNotEmpty())
        binding.gamePlayerEmptyGroup.setVisible(settings.players.isEmpty())

        val players = settings.players.map {
            PlayerState(
                it,
                it.id == settings.initialDealerId,
                // The first dealer can change until the game has a second round
                settings.hasDealer && (settings.id == null || settings.roundCount <= 1)
            )
        }
        (binding.playerRecycler.adapter as? PlayersAdapter)?.submitList(players)


        val checkedListener = OnCheckedChangeListener { checkbox: View, checked: Boolean ->
            logEvent(AnalyticsConstants.Event.TOGGLE_GAME_SETTING) {
                putString(
                    AnalyticsConstants.Param.ITEM_NAME,
                    checkbox.resources.getResourceEntryName((checkbox.parent as View).id)
                )
                putBoolean(AnalyticsConstants.Param.MESSAGE, checked)
            }
            when (checkbox) {
                _settingsBinding?.hasDealerCheckbox -> viewModel.setHasDealer(checked)
                _settingsBinding?.reversedScoringCheckbox -> viewModel.setReverseScoring(checked)
                _settingsBinding?.showNotesCheckbox -> viewModel.setShowNotes(checked)
                _settingsBinding?.useCalculatorCheckbox -> viewModel.setUseCalculator(checked)
            }
        }

        _settingsBinding?.hasDealerCheckbox?.isCheckedSafe(settings.hasDealer, checkedListener)
        _settingsBinding?.reversedScoringCheckbox?.isCheckedSafe(
            settings.reversedScoring,
            checkedListener
        )
        _settingsBinding?.showNotesCheckbox?.isCheckedSafe(settings.showRoundNotes, checkedListener)
        _settingsBinding?.useCalculatorCheckbox?.isCheckedSafe(
            settings.useCalculator,
            checkedListener
        )

        // Typing keeps the name the same as the game's, so this only changes it when the game changed on its own (e.g.
        // previous settings loading, or the game coming back after Android closed the app), even while it's focused
        renderText(binding.gameNameEdit, settings.name)

        // If we haven't focused the game name, focus that unless an edit text is focused already
        if (settings.name.isBlank() && !hasFocusedGameName && !binding.gameNameEdit.hasFocus() &&
            _settingsBinding?.maxScoreEdit?.hasFocus() != true && _settingsBinding?.maxRoundsEdit?.hasFocus() != true
        ) {
            hasFocusedGameName = true
            binding.gameNameEdit.requestFocus()
            binding.gameNameEdit.post {
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(binding.gameNameEdit, 0)
            }
        }

        renderTargetFields(settings)
    }

    /**
     * Shows max score/rounds, but leaves one being typed in alone while it means what's saved (a typed "0" is saved
     * as no limit, and the field clears once it loses focus)
     */
    private fun renderTargetFields(settings: GameSettings) {
        val settingsBinding = _settingsBinding ?: return
        val score = settingsBinding.maxScoreEdit
        if (!score.hasFocus() || ScoreFormat.parse(score.text.toString())?.takeIf { it != 0.0 } != settings.maxScore) {
            renderText(score, settings.maxScore?.let { ScoreFormat.format(it) } ?: "")
        }
        val rounds = settingsBinding.maxRoundsEdit
        if (!rounds.hasFocus() || rounds.text.toString().toIntOrNull()?.takeIf { it > 0 } != settings.maxRounds) {
            renderText(rounds, settings.maxRounds?.toString() ?: "")
        }
    }

    /** Shows the game's text in a field, without it reading as typed. The fields don't keep their own text (see layouts). */
    private fun renderText(field: EditText, text: String) {
        if (field.text.toString() == text) return
        isRenderingText = true
        // Without filtering, which would open the game name suggestions
        if (field is AutoCompleteTextView) field.setText(text, false) else field.setText(text)
        if (field.hasFocus()) field.setSelection(text.length)
        isRenderingText = false
    }

    // TODO: Handle swipe to remove a player
    private fun createItemTouchHelperCallback(): ItemTouchHelper.Callback =
        object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                viewModel.movePlayer(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    (viewHolder as? PlayerSelectionListener)?.onSelected()
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                (viewHolder as? PlayerSelectionListener)?.onCleared()
            }

            // TODO: Handle swipe to remove a player
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        }

    companion object {
        private const val ARG_GAME_ID = "game_id"
        private const val ARG_GAME_COPY = "game_id_copy"
        private const val ARG_GAME_INITIAL_DEALER_ID = "initial_dealer_id"
        private const val ARG_OPEN_GAME_ON_SAVE = "open_game_on_save"

        /**
         * @param openGameOnSave show the game once it's saved instead of going back, used when a game has to
         * be set up again before it can be played (e.g. all of its players were deleted)
         */
        fun newInstance(gameId: Long? = null, openGameOnSave: Boolean = false): GameSetupFragment {
            return GameSetupFragment().apply {
                gameId?.let {
                    arguments = Bundle().apply {
                        putLong(ARG_GAME_ID, it)
                        putBoolean(ARG_OPEN_GAME_ON_SAVE, openGameOnSave)
                    }
                }
            }
        }

        fun newInstanceCopy(gameId: Long, initialDealerId: Long?): GameSetupFragment {
            return GameSetupFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_GAME_COPY, gameId)
                    initialDealerId?.let { putLong(ARG_GAME_INITIAL_DEALER_ID, it) }
                }
            }
        }
    }
}

/**
 ****************************************
 * Player Adapter classes and interfaces
 ****************************************
 */

private interface PlayerSelectionListener {
    fun onSelected()
    fun onCleared()
}

private interface PlayerAdapterCallback {
    fun onSelectedDealer(playerId: Long)
}

private data class PlayerState(
    val player: Player,
    val isDealer: Boolean,
    val showDealer: Boolean
)

private class PlayerDiffCallback : DiffUtil.ItemCallback<PlayerState>() {
    override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState) =
        oldItem.player.id == newItem.player.id

    override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem == newItem
}

private class PlayersAdapter(private val callback: PlayerAdapterCallback) :
    ListAdapter<PlayerState, PlayerViewHolder>(PlayerDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(parent, callback)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).player.id!!
    }
}

private class PlayerViewHolder(parent: ViewGroup, val callback: PlayerAdapterCallback) :
    BaseViewHolder<HolderGameCreatePlayerBinding>(
        HolderGameCreatePlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ),
    PlayerSelectionListener {
    fun bind(state: PlayerState) {
        with(binding) {
            playerNameHolder.text = state.player.name
            playerDealerLabel.visibility =
                if (state.showDealer && state.isDealer) View.VISIBLE else View.GONE
            playerDealerBox.visibility = if (state.showDealer) View.VISIBLE else View.GONE
            playerDealerBox.isChecked = state.isDealer

            val selectDealer = View.OnClickListener {
                logEvent(AnalyticsConstants.Event.DEALER_REPLACED)
                callback.onSelectedDealer(state.player.id!!)
            }
            playerDealerBox.setOnClickListener(selectDealer)

            // The whole row picks the dealer, and screen readers hear it as one radio button named after the
            // player (the radio button and label inside are only visual)
            root.setOnClickListener(if (state.showDealer) selectDealer else null)
            root.isClickable = state.showDealer
            root.isFocusable = state.showDealer
            ViewCompat.setAccessibilityDelegate(root, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (!state.showDealer) return
                    info.className = RadioButton::class.java.name
                    info.isCheckable = true
                    info.isChecked = state.isDealer
                    info.hintText = host.context.getString(R.string.dealerLabel)
                }
            })
        }
    }

    override fun onSelected() {
        itemView.setBackgroundColor(itemView.context.themeColor(R.attr.colorSurfaceContainerHighest))
    }

    override fun onCleared() {
        itemView.setBackgroundRipple()
    }
}
