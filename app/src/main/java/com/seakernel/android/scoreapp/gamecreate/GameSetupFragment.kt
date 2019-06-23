package com.seakernel.android.scoreapp.gamecreate

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import kotlinx.android.synthetic.main.fragment_game_create.*
import kotlinx.android.synthetic.main.holder_player_select_list.view.*

class GameSetupFragment : Fragment() {

    interface GameSetupListener {
        fun onShowPlayerSelectScreen(playerIds: List<Long>)
        fun onShowGameScreen(gameId: Long)
    }

    private var listener: GameSetupListener? = null
    private var nameTextWatcher: TextWatcher? = null
    private val viewModel: GameSetupViewModel by lazy { ViewModelProviders.of(this).get(GameSetupViewModel::class.java) }

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
            adapter.submitList(settings.players)
        })
        viewModel.getGameCreatedEvent().observe(this, Observer { gameId ->
            listener?.onShowGameScreen(gameId)
        })
    }

    fun updateForNewPlayers(playerIds: List<Long>) {
        viewModel.updateForNewPlayers(playerIds)
    }

    companion object {
        fun newInstance(): GameSetupFragment {
            return GameSetupFragment()
        }
    }
}

private class PlayerDiffCallback : DiffUtil.ItemCallback<Player>() {
    override fun areItemsTheSame(oldItem: Player, newItem: Player) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Player, newItem: Player) = oldItem == newItem
}
class PlayersAdapter : ListAdapter<Player, PlayerViewHolder>(PlayerDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.holder_player_select_list, parent, false))
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(player: Player) {
        with (itemView) {
            playerNameHolder.text = player.name
        }
    }
}
