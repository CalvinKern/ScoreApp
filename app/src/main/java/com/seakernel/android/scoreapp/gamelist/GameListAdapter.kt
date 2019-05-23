package com.seakernel.android.scoreapp.gamelist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.SimpleGame
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.holder_game_list.view.*

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameListAdapter(private val gameList: List<SimpleGame>, private val eventConsumer: Consumer<ListEvent>) : RecyclerView.Adapter<GameListViewHolder>() {

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
        return gameList[position].id
    }
}

class GameListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val nameHolder: TextView by lazy { itemView.gameNameHolder }
    private val dateHolder: TextView by lazy { itemView.gameDateHolder }
    private val playersHolder: TextView by lazy { itemView.gamePlayersHolder }

    fun bind(game: SimpleGame, eventConsumer: Consumer<ListEvent>) {
        nameHolder.text = game.name
        dateHolder.text = game.lastPlayedAt
        playersHolder.text = itemView.context.getString(R.string.playersHolder, game.players.size)

        itemView.setOnClickListener { eventConsumer.accept(ListEvent.GameRowClicked(game.id)) }
        itemView.setOnLongClickListener {
            eventConsumer.accept(ListEvent.GameRowLongPressed(game.id))
            return@setOnLongClickListener true
        }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_game_list
    }
}