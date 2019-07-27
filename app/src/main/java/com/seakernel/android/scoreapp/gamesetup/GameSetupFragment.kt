package com.seakernel.android.scoreapp.gamesetup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.*
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.SimpleGame
import com.seakernel.android.scoreapp.utility.isCheckedSafe
import kotlinx.android.synthetic.main.fragment_game_create.*
import kotlinx.android.synthetic.main.holder_game_create_player.view.*

class GameSetupFragment : Fragment() {

    interface GameSetupListener {
        fun onShowPlayerSelectScreen(playerIds: List<Long>)
        fun onShowGameScreen(gameId: Long)
        fun onGameUpdated()
    }

    private var listener: GameSetupListener? = null
    private var nameTextWatcher: TextWatcher? = null
    private var hasDealerListener: CompoundButton.OnCheckedChangeListener? = null

    private val gameUpdatedObserver = Observer<Long> { listener?.onGameUpdated() }
    private val gameCreatedObserver = Observer<Long> { gameId -> listener?.onShowGameScreen(gameId) }
    private val modelObserver = Observer<SimpleGame?> { settings -> renderSettings(settings) }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_create, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        playersHeaderEdit?.setOnClickListener(null)
        hasDealerCheckbox?.setOnCheckedChangeListener(null)
        gameNameEdit?.removeTextChangedListener(nameTextWatcher)

        viewModel.getGameSettings().removeObserver(modelObserver)
        viewModel.getGameCreatedEvent().removeObserver(gameCreatedObserver)
        viewModel.getGameUpdatedEvent().removeObserver(gameUpdatedObserver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup toolbar
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

        // Setup recycler
        val adapter = PlayersAdapter(object : PlayerAdapterCallback {
            override fun onSelectedDealer(playerId: Long) {
                viewModel.setDealer(playerId)
            }
        })
        playerRecycler.layoutManager = LinearLayoutManager(requireContext())
        playerRecycler.adapter = adapter
        ItemTouchHelper(createItemTouchHelperCallback()).attachToRecyclerView(playerRecycler)

        // Setup various views
        playersHeaderEdit.setOnClickListener {
            val ids = viewModel.getGameSettings().value?.players?.map { it.id } ?: emptyList()
            listener?.onShowPlayerSelectScreen(ids)
        }

        nameTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(text: Editable?) {
                viewModel.updateGameName(text.toString())
            }
        }
        gameNameEdit.addTextChangedListener(nameTextWatcher)

        hasDealerListener = CompoundButton.OnCheckedChangeListener { _, checked ->
            viewModel.setHasDealer(checked)
        }
        hasDealerCheckbox.setOnCheckedChangeListener(hasDealerListener)

        // Start observing the data
        viewModel.getGameSettings().observe(this, modelObserver)
        viewModel.getGameCreatedEvent().observe(this, gameCreatedObserver)
        viewModel.getGameUpdatedEvent().observe(this, gameUpdatedObserver)

        arguments?.getLong(ARG_GAME_ID)?.let {
            viewModel.loadGame(it)
        } ?: viewModel.initializeGame()
    }

    fun updateForNewPlayers(playerIds: List<Long>) {
        viewModel.updateForNewPlayers(playerIds)
    }

    private fun renderSettings(settings: SimpleGame?) {
        if (settings == null) {
            return // TODO: Show loading spinner
        }
        toolbar.menu.findItem(R.id.actionSave).isEnabled = settings.name.isNotBlank() && settings.players.isNotEmpty()

        val players = settings.players.map {
            PlayerState(
                it,
                it.id == settings.initialDealerId,
                settings.hasDealer && settings.id == 0L // Don't show dealer for games that have started
            )
        }
        (playerRecycler.adapter as? PlayersAdapter)?.submitList(players)

        hasDealerCheckbox.isCheckedSafe(settings.hasDealer, hasDealerListener)

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

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
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

// Player Adapter classes

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
    override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem.player.id == newItem.player.id
    override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem == newItem
}

private class PlayersAdapter(private val callback: PlayerAdapterCallback) :
    ListAdapter<PlayerState, PlayerViewHolder>(PlayerDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.holder_game_create_player, parent, false)
        return PlayerViewHolder(view, callback)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).player.id
    }
}

private class PlayerViewHolder(itemView: View, val callback: PlayerAdapterCallback) : RecyclerView.ViewHolder(itemView),
    PlayerSelectionListener {
    fun bind(state: PlayerState) {
        with(itemView) {
            playerNameHolder.text = state.player.name
            playerDealerLabel.visibility = if (state.showDealer && state.isDealer) View.VISIBLE else View.GONE
            playerDealerBox.visibility = if (state.showDealer) View.VISIBLE else View.GONE
            playerDealerBox.isChecked = state.isDealer
            playerDealerBox.setOnClickListener {
                callback.onSelectedDealer(state.player.id)
            }
        }
    }

    override fun onSelected() {
        itemView.setBackgroundColor(Color.LTGRAY)
    }

    override fun onCleared() {
        itemView.setBackgroundColor(Color.WHITE)
    }
}
