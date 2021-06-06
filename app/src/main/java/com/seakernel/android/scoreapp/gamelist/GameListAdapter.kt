package com.seakernel.android.scoreapp.gamelist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.GameSettings
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.logEvent
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.holder_game_list.view.*

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameListAdapter(private val gameList: List<GameSettings>, private val eventConsumer: Consumer<ListEvent>) : RecyclerView.Adapter<GameListViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(GameListViewHolder.RESOURCE_ID, parent, false)
        return GameListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gameList.count()
    }

    override fun onBindViewHolder(holder: GameListViewHolder, position: Int) {
        holder.bind(gameList[position], eventConsumer)
    }

    override fun getItemId(position: Int): Long {
        return gameList[position].id!!
    }
}

class GameListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val nameHolder: TextView by lazy { itemView.gameNameHolder }
    private val dateHolder: TextView by lazy { itemView.gameDateHolder }
    private val playersHolder: TextView by lazy { itemView.gamePlayersHolder }

    fun bind(settings: GameSettings, eventConsumer: Consumer<ListEvent>) {
        nameHolder.text = settings.name
        dateHolder.text = settings.lastPlayedAt
        playersHolder.text = itemView.context.getString(R.string.playersHolder, settings.players.size)

        itemView.setOnClickListener {
            logEvent(AnalyticsConstants.Event.GAME_LOADED) {
                putString(AnalyticsConstants.Param.ITEM_NAME, settings.name)
                putString(AnalyticsConstants.Param.GAME_PLAYER_COUNT, settings.players.count().toString())
            }
            eventConsumer.accept(ListEvent.GameRowClicked(settings.id!!))
        }
        itemView.setOnLongClickListener {
            eventConsumer.accept(ListEvent.GameRowLongPressed(settings.id!!))
            return@setOnLongClickListener true
        }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_game_list
    }
}