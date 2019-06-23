package com.seakernel.android.scoreapp.gamecreate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.holder_player_list.view.*

class GameSetupFragment : Fragment() {

    interface GameSetupListener {
        fun onShowPlayerSelectScreen(playerIds: List<Long>)
    }

    private var listener: GameSetupListener? = null
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = PlayersAdapter()
        playerRecycler.layoutManager = LinearLayoutManager(requireContext())
        playerRecycler.adapter = adapter
        viewModel.getGameSettings().observe(this, Observer { settings ->
            adapter.submitList(settings.players)
        })

        playersHeaderEdit.setOnClickListener {
            val ids = viewModel.getGameSettings().value?.players?.map { it.id } ?: emptyList()
            listener?.onShowPlayerSelectScreen(ids)
        }

        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        // TODO: Set toolbar menu
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
        return PlayerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.holder_player_list, parent, false))
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
