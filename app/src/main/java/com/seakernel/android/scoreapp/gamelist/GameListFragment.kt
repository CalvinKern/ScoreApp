package com.seakernel.android.scoreapp.gamelist

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.seakernel.android.scoreapp.utility.setVisible
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameListFragment : MobiusFragment<ListModel, ListEvent, ListEffect>() {

    interface GameListListener {
        fun onShowGameScreen(gameId: Long)
        fun onShowCreateGameScreen()
    }

    override val layoutId = R.layout.fragment_game_list

    private var gameRepository: GameRepository? = null
    private var listener: GameListListener? = null

    init {
        loop = Mobius.loop(ListModel.Companion::update, ::effectHandler).init(::initMobius)
        controller = MobiusAndroid.controller(loop, ListModel.createDefault())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        gameRepository = GameRepository(requireContext())
        listener = context as? GameListListener
    }

    override fun onDetach() {
        super.onDetach()

        gameRepository = null
        listener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup views
        gameRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.gameListTitle)
    }
    // Mobius functions

    override fun initMobius(model: ListModel): First<ListModel, ListEffect> {
        return First.first(model.copy(isLoading = true), setOf(ListEffect.FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<ListEvent>): Connection<ListModel> {
        // Send events to the consumer when the button is pressed
        fab.setOnClickListener {
            eventConsumer.accept(ListEvent.AddGameClicked)
        }

        return object : Connection<ListModel> {
            override fun accept(model: ListModel) {
                gameListLoading.setVisible(model.isLoading)
                gameRecycler.setVisible(!model.isLoading && model.gameList.isNotEmpty())
                gameListEmptyGroup.setVisible(!model.isLoading && model.gameList.isEmpty())

                gameRecycler.swapAdapter(GameListAdapter(model.gameList, eventConsumer), true)
            }

            override fun dispose() {
                // Don't forget to remove listeners when the UI is disconnected
                fab.setOnClickListener(null)
                gameRecycler.swapAdapter(null, true)
            }
        }
    }

    override fun effectHandler(eventConsumer: Consumer<ListEvent>): Connection<ListEffect> {
        return object : Connection<ListEffect> {
            override fun accept(effect: ListEffect) {
                when (effect) {
                    is ListEffect.ShowCreateGameScreen -> listener?.onShowCreateGameScreen()
                    is ListEffect.ShowGameScreen -> listener?.onShowGameScreen(effect.gameId)
                    is ListEffect.ShowGameRowDialog -> {
                        showDeleteGameDialog(eventConsumer, effect.gameId)
                    }
                    is ListEffect.ShowDeleteSnackbar -> {
                    }
                    is ListEffect.FetchData -> {
                        eventConsumer.accept(ListEvent.Loaded(gameRepository?.loadAllGames() ?: listOf()))
                    }
                }
            }

            override fun dispose() {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    private fun showDeleteGameDialog(eventConsumer: Consumer<ListEvent>, gameId: Long) {
        view?.post {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(R.string.deleteGameConfirmation)
                .setPositiveButton(R.string.delete) { _, _ ->
                    deleteGameAsync(eventConsumer, gameId)
                }
                .setNegativeButton(android.R.string.cancel, null)
            builder.create().show()
        }
    }

    private fun deleteGameAsync(eventConsumer: Consumer<ListEvent>, gameId: Long) {
        GlobalScope.launch {
            if (gameRepository?.deleteGame(gameId) == true) {
                eventConsumer.accept(ListEvent.GameDeleteSuccessful(gameId))
            } else {
                // TODO: Show error, shouldn't happen, but why not catch it?
                Toast.makeText(requireContext(), R.string.delete, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // End Mobius functions

    companion object {
        fun newInstance(): GameListFragment {
            return GameListFragment()
        }
    }
}