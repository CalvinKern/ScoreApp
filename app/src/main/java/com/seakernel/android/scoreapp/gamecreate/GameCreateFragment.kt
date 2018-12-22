package com.seakernel.android.scoreapp.gamecreate

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.gamecreate.CreateModel.Companion.update
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game_create.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameCreateFragment : MobiusFragment<CreateModel, CreateEvent, CreateEffect>() {

    interface GameCreateListener {
        fun onShowGameScreen(gameId: Long)
    }

    override val layoutId = R.layout.fragment_game_create

    private var playerRepository: PlayerRepository? = null
    private var gameRepository: GameRepository? = null
    private var listener: GameCreateListener? = null
    private var addPlayerJob: Job? = null

    private lateinit var nameTextWatcher: TextWatcher
    private lateinit var toolbarItemClickListener: Toolbar.OnMenuItemClickListener

    init {
        loop = Mobius.loop(::update, ::effectHandler).init(::initMobius)
        controller = MobiusAndroid.controller(loop, CreateModel.createDefault())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        playerRepository = PlayerRepository(requireContext())
        gameRepository = GameRepository(requireContext())

        listener = context as? GameCreateListener
    }

    override fun onDetach() {
        super.onDetach()
        playerRepository = null
        gameRepository = null
        listener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // TODO: Retrieve selected state

        // Setup views
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() /* TODO: Verify leaving the new game? */ }
        toolbar.inflateMenu(R.menu.menu_game_create)
        playerRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gameNameEdit.removeTextChangedListener(nameTextWatcher)
        addPlayerJob?.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // TODO: Store selected state
    }

    // Mobius functions

    override fun initMobius(model: CreateModel): First<CreateModel, CreateEffect> {
        return First.first(model, setOf(FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<CreateEvent>): Connection<CreateModel> {
        nameTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                eventConsumer.accept(GameNameChanged(p0?.toString() ?: ""))
            }
        }

        toolbarItemClickListener = Toolbar.OnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.actionSave -> eventConsumer.accept(StartGameClicked)
            }
            false
        }

        fab.setOnClickListener { _ ->
            eventConsumer.accept(AddPlayerClicked)
        }
        gameNameEdit.addTextChangedListener(nameTextWatcher)
        toolbar.setOnMenuItemClickListener(toolbarItemClickListener)

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

    override fun effectHandler(eventConsumer: Consumer<CreateEvent>): Connection<CreateEffect> {
        return object : Connection<CreateEffect> {
            override fun accept(effect: CreateEffect) {
                when (effect) {
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
                    is SaveGame -> {
                        gameRepository?.createGame(effect.gameName, effect.playerIds)?.let {
                            listener?.onShowGameScreen(it)
                        }
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
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.playerName)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (name.isNotEmpty()) {
                    addPlayerJob = GlobalScope.launch {
                        playerRepository?.addOrUpdateUser(effect.playerId, name)?.let { player ->
                            eventConsumer.accept(PlayerNameChanged(player.id, player.name))
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        (view as? EditText)?.apply {
            setText(effect.playerName)
            setSelection(effect.playerName.length)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    name = p0?.toString() ?: ""
                    dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = name.isNotEmpty()
                }
            })
        }

        // Disable "ok" button until there is text in the dialog
        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = effect.playerName.isNotEmpty()
        }
        dialog.show()
    }

    companion object {
        fun newInstance() : GameCreateFragment {
            return GameCreateFragment()
        }
    }
}