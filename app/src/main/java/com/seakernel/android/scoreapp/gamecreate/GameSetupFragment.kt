package com.seakernel.android.scoreapp.gamecreate

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.*
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import kotlinx.android.synthetic.main.fragment_game_create.*
import kotlinx.android.synthetic.main.holder_game_create_player.view.*

class GameSetupFragment : Fragment() {

    interface GameSetupListener {
        fun onShowPlayerSelectScreen(playerIds: List<Long>)
        fun onShowGameScreen(gameId: Long)
    }

    private var listener: GameSetupListener? = null
    private var nameTextWatcher: TextWatcher? = null

    // Require activity so that we can share updates with the activity and the view holders (unless it's refactored to use a callback instead of accessing the viewmodel directly)
    private val viewModel: GameSetupViewModel by lazy { ViewModelProviders.of(requireActivity()).get(GameSetupViewModel::class.java) }

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
        gameNameEdit?.removeTextChangedListener(nameTextWatcher)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup toolbar
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() /* TODO: Verify leaving the new settings? */ }
        toolbar.inflateMenu(R.menu.menu_game_create)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSave -> {
                    viewModel.createGame()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }

        // Setup recycler
        val adapter = PlayersAdapter()
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

        // Start observing the data
        viewModel.getGameSettings().observe(this, Observer { settings ->
            val players = settings.players.map { PlayerState(it, it.id == settings.initialDealerId) }
            adapter.submitList(players)
        })
        viewModel.getGameCreatedEvent().observe(this, Observer { gameId ->
            listener?.onShowGameScreen(gameId)
        })
    }

    fun updateForNewPlayers(playerIds: List<Long>) {
        viewModel.updateForNewPlayers(playerIds)
    }

    // TODO: Handle swipe to remove a player
    private fun createItemTouchHelperCallback(): ItemTouchHelper.Callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            viewModel.movePlayer(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        // TODO: Handle swipe to remove a player
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    }

    companion object {
        fun newInstance(): GameSetupFragment {
            return GameSetupFragment()
        }
    }
}

// Player Adapter classes

private class PlayerDiffCallback : DiffUtil.ItemCallback<PlayerState>() {
    override fun areItemsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem.player.id == newItem.player.id
    override fun areContentsTheSame(oldItem: PlayerState, newItem: PlayerState) = oldItem == newItem
}

class PlayersAdapter : ListAdapter<PlayerState, PlayerViewHolder>(PlayerDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.holder_game_create_player, parent, false))
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(state: PlayerState) {
        with (itemView) {
            playerNameHolder.text = state.player.name
            playerDealerLabel.visibility = if (state.isDealer) View.VISIBLE else View.GONE
            playerDealerBox.isChecked = state.isDealer
            playerDealerBox.setOnClickListener {
                ViewModelProviders.of(itemView.context as FragmentActivity).get(GameSetupViewModel::class.java).setDealer(state.player.id)
            }
        }
    }
}

data class PlayerState(
    val player: Player,
    val isDealer: Boolean
)
