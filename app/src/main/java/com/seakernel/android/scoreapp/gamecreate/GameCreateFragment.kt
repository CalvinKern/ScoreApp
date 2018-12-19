package com.seakernel.android.scoreapp.gamecreate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.gamecreate.CreateModel.Companion.update
import com.spotify.mobius.Connection
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game_create.*
import kotlinx.android.synthetic.main.holder_game_list.view.*
import kotlinx.android.synthetic.main.holder_player_list.view.*

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameCreateFragment : Fragment() {

    private val loop = Mobius.loop(::update, ::effectHandler)
    private val controller = MobiusAndroid.controller(loop, CreateModel.createDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_create, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.disconnect()
    }

    override fun onStart() {
        super.onStart()
        controller.start()
    }

    override fun onPause() {
        super.onPause()
        controller.stop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup views
        playerRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Setup Mobius
        controller.connect(::connectViews)
    }

    // Mobius functions

    private fun connectViews(eventConsumer: Consumer<CreateEvent>): Connection<CreateModel> {
        // Send events to the consumer when the button is pressed
        fab.setOnClickListener { _ ->
            eventConsumer.accept(AddPlayerClicked)
        }

        return object : Connection<CreateModel> {
            override fun accept(model: CreateModel) {
                // This will be called whenever there is a new model
                playerRecycler.swapAdapter(PlayerListAdapter(model.playerList, eventConsumer), true)
            }

            override fun dispose() {
                // Don't forget to remove listeners when the UI is disconnected
                fab.setOnClickListener(null)
                playerRecycler.swapAdapter(null, true)
            }
        }
    }

    private fun effectHandler(eventConsumer: Consumer<CreateEvent>): Connection<CreateEffect> {
        return object : Connection<CreateEffect> {
            override fun accept(value: CreateEffect) {
                when (value) {
                    is ShowGameScreen -> {}
                    is ShowPlayerNameDialog -> {}
                    is ShowDeleteDialog -> {}
                    is ShowDeletePlayerSnackbar -> {}
                }
            }

            override fun dispose() {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    // End Mobius functions

    companion object {
        fun newInstance() : GameCreateFragment {
            return GameCreateFragment()
        }
    }
}

private class PlayerListAdapter(private val playerList: List<Player>, private val eventConsumer: Consumer<CreateEvent>) : RecyclerView.Adapter<PlayerListViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(PlayerListViewHolder.RESOURCE_ID, parent, false)
        return PlayerListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playerList.count()
    }

    override fun onBindViewHolder(holder: PlayerListViewHolder, position: Int) {
        holder.bind(playerList[position], eventConsumer)
    }

    override fun getItemId(position: Int): Long {
        return playerList[position].id
    }
}

private class PlayerListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val nameHolder: TextView by lazy { itemView.gameNameHolder }
    val deleteHolder: View by lazy { itemView.playerDeleteHolder }

    fun bind(player: Player, eventConsumer: Consumer<CreateEvent>) {
        nameHolder.text = player.name
        itemView.setOnClickListener { eventConsumer.accept(PlayerRowClicked(player.id)) }
        deleteHolder.setOnClickListener { eventConsumer.accept(PlayerDeleteClicked(player.id)) }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_player_list
    }
}