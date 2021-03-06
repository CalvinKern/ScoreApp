package com.seakernel.android.scoreapp.playerselect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.logEvent
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.holder_player_select_list.view.*

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class PlayerListAdapter(private val playerList: List<Player>, private val selectedPlayerIds: List<Long>, private val eventConsumer: Consumer<PlayerEvent>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlayerListViewHolder(
        LayoutInflater.from(parent.context).inflate(PlayerListViewHolder.RESOURCE_ID, parent, false)
    )

    override fun getItemCount(): Int {
        return playerList.count()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val player = playerList[position]
        (holder as PlayerListViewHolder).bind(player, selectedPlayerIds.contains(player.id), eventConsumer)
    }

    override fun getItemId(position: Int) = playerList[position].id ?: Long.MAX_VALUE
}

class PlayerListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val nameHolder: TextView by lazy { itemView.playerNameHolder }
    private val checkHolder: MaterialCheckBox by lazy { itemView.playerCheckHolder }
    private val settingsButton: View by lazy { itemView.playerNameSettings }

    fun bind(player: Player, isSelected: Boolean, eventConsumer: Consumer<PlayerEvent>) {
        nameHolder.text = player.name

        checkHolder.setOnCheckedChangeListener(null)
        checkHolder.isChecked = isSelected
        checkHolder.setOnCheckedChangeListener { _, selected ->
            logEvent(AnalyticsConstants.Event.GAME_PLAYER_ADDED)
            eventConsumer.accept(PlayerSelected(player.id!!, selected))
        }

        settingsButton.setOnClickListener { showPlayerDialog(player.id!!, eventConsumer) }
        itemView.setOnClickListener { checkHolder.performClick() }
        itemView.setOnLongClickListener {
            showPlayerDialog(player.id!!, eventConsumer)
            true
        }
    }

    private fun showPlayerDialog(playerId: Long, eventConsumer: Consumer<PlayerEvent>) {
        eventConsumer.accept(PlayerRowLongClicked(playerId))
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_player_select_list
    }
}
