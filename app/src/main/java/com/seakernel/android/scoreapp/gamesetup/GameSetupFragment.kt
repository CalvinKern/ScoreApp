package com.seakernel.android.scoreapp.gamesetup

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.FragmentGameCreateBinding
import com.seakernel.android.scoreapp.databinding.HolderGameCreatePlayerBinding
import com.seakernel.android.scoreapp.databinding.ViewGameSettingsBinding
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.isCheckedSafe
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setBackgroundRipple
import com.seakernel.android.scoreapp.utility.setVisible

class GameSetupFragment : Fragment() {

    interface GameSetupListener {
        fun onShowPlayerSelectScreen(playerIds: List<Long>)
        fun onShowGameScreen(gameId: Long)
        fun onGameUpdated()
    }

    private var listener: GameSetupListener? = null
    private var nameTextWatcher: TextWatcher? = null

    private val gameUpdatedObserver = Observer<Long> { listener?.onGameUpdated() }
    private val gameCreatedObserver =
        Observer<Long> { gameId -> listener?.onShowGameScreen(gameId) }
    private val saveObserver =
        Observer<Boolean?> { saving -> renderSettings(viewModel.getGameSettings().value, saving) }
    private val modelObserver =
        Observer<GameSettings?> { settings -> renderSettings(settings, viewModel.isSaving().value) }
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

        _binding?.gameNameEdit?.removeTextChangedListener(nameTextWatcher)

        viewModel.getGameSettings().removeObserver(modelObserver)
        viewModel.getGameCreatedEvent().removeObserver(gameCreatedObserver)
        viewModel.getGameUpdatedEvent().removeObserver(gameUpdatedObserver)
        viewModel.getGameNamesForAutocomplete().removeObserver(autocompleteObserver)

        _binding = null
        _settingsBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup toolbar
        initToolbar()
        initSettings()

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.toolbar.applyWindowInsetsCutout()
        binding.gameEditContainer.applyWindowInsetsNavigationPadding(includeBottom = false)
        binding.playerRecycler.applyWindowInsetsNavigationPadding()

        // Start observing the data
        viewModel.getGameSettings().observe(viewLifecycleOwner, modelObserver)
        viewModel.getGameCreatedEvent().observe(viewLifecycleOwner, gameCreatedObserver)
        viewModel.getGameUpdatedEvent().observe(viewLifecycleOwner, gameUpdatedObserver)
        viewModel.getGameNamesForAutocomplete().observe(viewLifecycleOwner, autocompleteObserver)

        // Check for copy first, then opening a game
        if (arguments?.containsKey(ARG_GAME_COPY) == true) {
            viewModel.createCopy(
                requireArguments().getLong(ARG_GAME_COPY),
                requireArguments().getLong(ARG_GAME_INITIAL_DEALER_ID)
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

    fun updateForNewPlayers(playerIds: List<Long>) {
        viewModel.updateForNewPlayers(playerIds)
    }

    private fun initToolbar() {
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() /* TODO: Verify leaving the new settings? */ }
        binding.toolbar.inflateMenu(R.menu.menu_game_create)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSave -> {
                    viewModel.saveGame()
                    true
                }

                else -> false
            }
        }
        binding.toolbar.setTitle(if (arguments?.containsKey(ARG_GAME_ID) == true) R.string.gameSettingsTitle else R.string.gameCreateTitle)
    }

    private fun initSettings() {
        initListeners()

        // Setup player recycler
        binding.playersHeaderEdit.setOnClickListener {
            val ids =
                viewModel.getGameSettings().value?.players?.mapNotNull { it.id } ?: emptyList()
            listener?.onShowPlayerSelectScreen(ids)
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
                viewModel.updateGameName(text.toString())
            }
        }

        val settingsBinding = _settingsBinding ?: return

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
        binding.toolbar.menu.findItem(R.id.actionSave).isEnabled =
            settings.name.isNotBlank() && settings.players.isNotEmpty() && isSaving != true
        binding.playerRecycler.setVisible(settings.players.isNotEmpty())
        binding.gamePlayerEmptyGroup.setVisible(settings.players.isEmpty())

        val players = settings.players.map {
            PlayerState(
                it,
                it.id == settings.initialDealerId,
                settings.hasDealer && settings.id == null // Don't show dealer for games that have started
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

        // Update game name unless it has focus (being edited)
        if (!binding.gameNameEdit.hasFocus()) {
            binding.gameNameEdit.removeTextChangedListener(nameTextWatcher)
            binding.gameNameEdit.setText(settings.name)
            binding.gameNameEdit.addTextChangedListener(nameTextWatcher)
        }
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

        fun newInstance(gameId: Long? = null): GameSetupFragment {
            return GameSetupFragment().apply {
                gameId?.let {
                    arguments = Bundle().apply {
                        putLong(ARG_GAME_ID, it)
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
            playerDealerBox.setOnClickListener {
                logEvent(AnalyticsConstants.Event.DEALER_REPLACED)
                callback.onSelectedDealer(state.player.id!!)
            }
        }
    }

    override fun onSelected() {
        itemView.setBackgroundColor(getColor(itemView.context, R.color.colorSelected))
    }

    override fun onCleared() {
        itemView.setBackgroundRipple()
    }
}
