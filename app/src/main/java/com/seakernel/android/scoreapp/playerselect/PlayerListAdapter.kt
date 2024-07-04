package com.seakernel.android.scoreapp.playerselect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.HolderPlayerSelectListBinding
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.logEvent
import com.spotify.mobius.functions.Consumer

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class PlayerListAdapter(
    private val playerList: List<Player>,
    private val selectedPlayerIds: List<Long>,
    private val eventConsumer: Consumer<PlayerEvent>,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlayerListViewHolder(parent)

    override fun getItemCount(): Int {
        return playerList.count()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val player = playerList[position]
        (holder as PlayerListViewHolder).bind(
            player,
            selectedPlayerIds.contains(player.id),
            eventConsumer
        )
    }

    override fun getItemId(position: Int) = playerList[position].id ?: Long.MAX_VALUE
}

class PlayerListViewHolder(parent: ViewGroup) :
    BaseViewHolder<HolderPlayerSelectListBinding>(
        HolderPlayerSelectListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) {

    fun bind(player: Player, isSelected: Boolean, eventConsumer: Consumer<PlayerEvent>) {
        binding.playerNameHolder.text = player.name

        binding.playerCheckHolder.setOnCheckedChangeListener(null)
        binding.playerCheckHolder.isChecked = isSelected
        binding.playerCheckHolder.setOnCheckedChangeListener { _, selected ->
            logEvent(AnalyticsConstants.Event.GAME_PLAYER_ADDED)
            eventConsumer.accept(PlayerSelected(player.id!!, selected))
        }

        binding.playerNameSettings.setOnClickListener {
            showPlayerDialog(
                player.id!!,
                eventConsumer
            )
        }
        itemView.setOnClickListener { binding.playerCheckHolder.performClick() }
        itemView.setOnLongClickListener {
            showPlayerDialog(player.id!!, eventConsumer)
            true
        }
    }

    private fun showPlayerDialog(playerId: Long, eventConsumer: Consumer<PlayerEvent>) {
        eventConsumer.accept(PlayerRowLongClicked(playerId))
    }
}
