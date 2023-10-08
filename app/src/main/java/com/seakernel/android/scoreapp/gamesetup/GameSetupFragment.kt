package com.seakernel.android.scoreapp.gamesetup

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.isCheckedSafe
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setBackgroundRipple
import com.seakernel.android.scoreapp.utility.setVisible
import kotlinx.android.synthetic.main.fragment_game_create.gameNameEdit
import kotlinx.android.synthetic.main.fragment_game_create.gamePlayerEmptyGroup
import kotlinx.android.synthetic.main.fragment_game_create.playerRecycler
import kotlinx.android.synthetic.main.fragment_game_create.playersHeaderEdit
import kotlinx.android.synthetic.main.fragment_game_create.toolbar
import kotlinx.android.synthetic.main.holder_game_create_player.view.playerDealerBox
import kotlinx.android.synthetic.main.holder_game_create_player.view.playerDealerLabel
import kotlinx.android.synthetic.main.holder_game_create_player.view.playerNameHolder
import kotlinx.android.synthetic.main.view_game_settings.hasDealerContainer
import kotlinx.android.synthetic.main.view_game_settings.reversedScoringContainer
import kotlinx.android.synthetic.main.view_game_settings.showNotesContainer
import kotlinx.android.synthetic.main.view_game_settings.useCalculatorContainer
import kotlinx.android.synthetic.main.view_game_settings.view.checkbox

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
    private val modelObserver = Observer<GameSettings?> { settings -> renderSettings(settings) }
    private val autocompleteObserver = Observer<List<String>?> { names ->
        gameNameEdit.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names?.distinct() ?: emptyList(),
            )
        )
    }

    private val viewModel: GameSetupViewModel by lazy {
        ViewModelProviders.of(this).get(GameSetupViewModel::class.java)
    }

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
    ): View? {
        return inflater.inflate(R.layout.fragment_game_create, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        playersHeaderEdit?.setOnClickListener(null)
        hasDealerContainer?.checkbox?.setOnCheckedChangeListener(null)
        hasDealerContainer?.setOnClickListener(null)
        reversedScoringContainer?.checkbox?.setOnCheckedChangeListener(null)
        reversedScoringContainer?.setOnClickListener(null)
        showNotesContainer?.checkbox?.setOnCheckedChangeListener(null)
        showNotesContainer?.setOnClickListener(null)
        useCalculatorContainer?.checkbox?.setOnCheckedChangeListener(null)
        useCalculatorContainer?.setOnClickListener(null)

        gameNameEdit?.removeTextChangedListener(nameTextWatcher)

        viewModel.getGameSettings().removeObserver(modelObserver)
        viewModel.getGameCreatedEvent().removeObserver(gameCreatedObserver)
        viewModel.getGameUpdatedEvent().removeObserver(gameUpdatedObserver)
        viewModel.getGameNamesForAutocomplete().removeObserver(autocompleteObserver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup toolbar
        initToolbar()
        initSettings()

        // Start observing the data
        viewModel.getGameSettings().observe(viewLifecycleOwner, modelObserver)
        viewModel.getGameCreatedEvent().observe(viewLifecycleOwner, gameCreatedObserver)
        viewModel.getGameUpdatedEvent().observe(viewLifecycleOwner, gameUpdatedObserver)
        viewModel.getGameNamesForAutocomplete().observe(viewLifecycleOwner, autocompleteObserver)

        arguments?.getLong(ARG_GAME_ID)?.let {
            viewModel.loadGame(it)
        } ?: viewModel.initializeGame()
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.GameSetupFragment)
    }

    fun updateForNewPlayers(playerIds: List<Long>) {
        viewModel.updateForNewPlayers(playerIds)
    }

    private fun initToolbar() {
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() /* TODO: Verify leaving the new settings? */ }
        toolbar.inflateMenu(R.menu.menu_game_create)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSave -> {
                    viewModel.saveGame()
                    true
                }

                else -> super.onOptionsItemSelected(item)
            }
        }
        toolbar.setTitle(if (arguments?.containsKey(ARG_GAME_ID) == true) R.string.gameSettingsTitle else R.string.gameCreateTitle)
    }

    private fun initSettings() {
        initListeners()

        // Setup player recycler
        playersHeaderEdit.setOnClickListener {
            val ids =
                viewModel.getGameSettings().value?.players?.mapNotNull { it.id } ?: emptyList()
            listener?.onShowPlayerSelectScreen(ids)
        }

        val adapter = PlayersAdapter(object : PlayerAdapterCallback {
            override fun onSelectedDealer(playerId: Long) {
                viewModel.setDealer(playerId)
            }
        })
        playerRecycler.layoutManager = LinearLayoutManager(requireContext())
        playerRecycler.adapter = adapter
        ItemTouchHelper(createItemTouchHelperCallback()).attachToRecyclerView(playerRecycler)
    }

    private fun initListeners() {
        nameTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                viewModel.updateGameName(text.toString())
            }
        }

        setListenerRow(hasDealerContainer)
        setListenerRow(reversedScoringContainer)
        setListenerRow(showNotesContainer)
        setListenerRow(useCalculatorContainer)
    }

    private fun setListenerRow(container: View) {
        container.setOnClickListener { container.checkbox.performClick() }
    }

    private fun renderSettings(settings: GameSettings?) {
        if (settings == null) {
            return // TODO: Show loading spinner
        }
        toolbar.menu.findItem(R.id.actionSave).isEnabled =
            settings.name.isNotBlank() && settings.players.isNotEmpty()
        playerRecycler.setVisible(settings.players.isNotEmpty())
        gamePlayerEmptyGroup.setVisible(settings.players.isEmpty())

        val players = settings.players.map {
            PlayerState(
                it,
                it.id == settings.initialDealerId,
                settings.hasDealer && settings.id == null // Don't show dealer for games that have started
            )
        }
        (playerRecycler.adapter as? PlayersAdapter)?.submitList(players)


        val checkedListener = OnCheckedChangeListener { checkbox: View, checked: Boolean ->
            logEvent(AnalyticsConstants.Event.TOGGLE_GAME_SETTING) {
                putString(
                    AnalyticsConstants.Param.ITEM_NAME,
                    checkbox.resources.getResourceEntryName((checkbox.parent as View).id)
                )
                putBoolean(AnalyticsConstants.Param.MESSAGE, checked)
            }
            when (checkbox) {
                hasDealerContainer.checkbox -> viewModel.setHasDealer(checked)
                reversedScoringContainer.checkbox -> viewModel.setReverseScoring(checked)
                showNotesContainer.checkbox -> viewModel.setShowNotes(checked)
                useCalculatorContainer.checkbox -> viewModel.setUseCalculator(checked)
            }
        }

        hasDealerContainer.checkbox.isCheckedSafe(settings.hasDealer, checkedListener)
        reversedScoringContainer.checkbox.isCheckedSafe(settings.reversedScoring, checkedListener)
        showNotesContainer.checkbox.isCheckedSafe(settings.showRoundNotes, checkedListener)
        useCalculatorContainer.checkbox.isCheckedSafe(settings.useCalculator, checkedListener)

        // Update game name unless it has focus (being edited)
        if (!gameNameEdit.hasFocus()) {
            gameNameEdit.removeTextChangedListener(nameTextWatcher)
            gameNameEdit.setText(settings.name)
            gameNameEdit.addTextChangedListener(nameTextWatcher)
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
                viewModel.movePlayer(viewHolder.adapterPosition, target.adapterPosition)
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

        fun newInstance(gameId: Long? = null): GameSetupFragment {
            return GameSetupFragment().apply {
                gameId?.let {
                    arguments = Bundle().apply {
                        putLong(ARG_GAME_ID, it)
                    }
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.holder_game_create_player, parent, false)
        return PlayerViewHolder(view, callback)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).player.id!!
    }
}

private class PlayerViewHolder(itemView: View, val callback: PlayerAdapterCallback) :
    RecyclerView.ViewHolder(itemView),
    PlayerSelectionListener {
    fun bind(state: PlayerState) {
        with(itemView) {
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
