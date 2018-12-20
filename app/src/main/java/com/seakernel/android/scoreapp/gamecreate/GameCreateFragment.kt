package com.seakernel.android.scoreapp.gamecreate

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.gamecreate.CreateModel.Companion.update
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game_create.*
import kotlinx.android.synthetic.main.holder_player_list.view.*

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameCreateFragment : Fragment() {

    private val loop = Mobius.loop(::update, ::effectHandler).init(::initMobius)
    private var controller: MobiusLoop.Controller<CreateModel, CreateEvent>? = null
    private var playerRepository: PlayerRepository? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        playerRepository = PlayerRepository(requireContext())
        controller = MobiusAndroid.controller(loop, CreateModel.createDefault())
    }

    override fun onDetach() {
        super.onDetach()
        playerRepository = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_create, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller?.disconnect()
    }

    override fun onStart() {
        super.onStart()
        controller?.start()
    }

    override fun onPause() {
        super.onPause()
        controller?.stop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup views
        playerRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Setup Mobius
        controller?.connect(::connectViews)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.gameCreateTitle)
    }

    // Mobius functions

    private fun initMobius(model: CreateModel): First<CreateModel, CreateEffect> {
        return First.first(model, setOf(FetchData))
    }

    private fun connectViews(eventConsumer: Consumer<CreateEvent>): Connection<CreateModel> {
        // Send events to the consumer when the button is pressed
        fab.setOnClickListener { _ ->
            eventConsumer.accept(AddPlayerClicked)
        }

        return object : Connection<CreateModel> {
            override fun accept(model: CreateModel) {
                // This will be called whenever there is a new model
                playerRecycler.swapAdapter(PlayerListAdapter(model.playerList, model.selectedPlayerList, eventConsumer), false)
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
            override fun accept(effect: CreateEffect) {
                when (effect) {
                    is ShowGameScreen -> {}
                    is ShowPlayerNameDialog -> {
                        view?.post {
                            showPlayerNameDialog(eventConsumer, effect)
                        }
                    }
                    is ShowDeleteDialog -> {
                        // TODO: Show dialog for confirm delete
//                        playerRepository?.deleteUser(effect.playerId)
//                        eventConsumer.accept(PlayerDeleteSuccessful(effect.playerId))
                    }
                    is ShowDeletePlayerSnackbar -> { /* TODO: Show snackbar to undo? or show a toast? */ }
                    is FetchData -> {
                        eventConsumer.accept(PlayersLoaded(playerRepository?.loadAllUsers() ?: listOf()))
                    }
                }
            }

            override fun dispose() {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    // End Mobius functions

    private fun showPlayerNameDialog(eventConsumer: Consumer<CreateEvent>, effect: ShowPlayerNameDialog) {
        var name = ""
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_player_name, null, false)
        (view as? EditText)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                name = p0?.toString() ?: ""
            }
        })
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.playerName)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (name.isNotEmpty()) {
//                    playerRepository?.addOrUpdateUser(effect.playerId, name)?.let { player ->
//                        eventConsumer.accept(PlayerNameChanged(player.id, player.name))
//                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    companion object {
        fun newInstance() : GameCreateFragment {
            return GameCreateFragment()
        }
    }
}

private class PlayerListAdapter(private val playerList: List<Player>, private val selectedPlayerIds: List<Long>, private val eventConsumer: Consumer<CreateEvent>) : RecyclerView.Adapter<PlayerListViewHolder>() {

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
        val player = playerList[position]
        holder.bind(player, selectedPlayerIds.contains(player.id), eventConsumer)
    }

    override fun getItemId(position: Int): Long {
        return playerList[position].id
    }
}

private class PlayerListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val nameHolder: TextView by lazy { itemView.playerNameHolder }
    val checkHolder: MaterialCheckBox by lazy { itemView.playerCheckHolder }

    fun bind(player: Player, isSelected: Boolean, eventConsumer: Consumer<CreateEvent>) {
        nameHolder.text = player.name

        checkHolder.setOnCheckedChangeListener(null)
        checkHolder.isChecked = isSelected
        checkHolder.setOnCheckedChangeListener { _, selected ->
            eventConsumer.accept(PlayerSelected(player.id, selected))
        }

        itemView.setOnClickListener { eventConsumer.accept(PlayerRowClicked(player.id)) }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_player_list
    }
}