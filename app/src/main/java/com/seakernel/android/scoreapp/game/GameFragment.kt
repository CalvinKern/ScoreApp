package com.seakernel.android.scoreapp.game

import android.content.Context
import android.os.Bundle
import android.view.View
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameFragment : MobiusFragment<GameModel, GameEvent, GameEffect>() {

    override val layoutId = R.layout.fragment_game

    private var gameRepository: GameRepository? = null
    private var loadGameJob: Job? = null

    init {
        loop = Mobius.loop(GameModel.Companion::update, ::effectHandler).init(::initMobius)
        controller = MobiusAndroid.controller(loop, GameModel.createDefault())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gameRepository = GameRepository(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        gameRepository = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // TODO: Restore state
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbar.setNavigationOnClickListener(null)
        loadGameJob?.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // TODO: Store state
    }

    // Mobius functions

    override fun initMobius(model: GameModel): First<GameModel, GameEffect> {
        return First.first(model, setOf(FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<GameEvent>): Connection<GameModel> {
        return object : Connection<GameModel> {
            override fun accept(model: GameModel) {
                loadGameJob = GlobalScope.launch(Dispatchers.Main) {
                    toolbar.title = model.game.name
                    gamePlayers.text = model.game.players.joinToString { it.name }
                }
            }

            override fun dispose() {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }

    override fun effectHandler(eventConsumer: Consumer<GameEvent>): Connection<GameEffect> {
        return object : Connection<GameEffect> {
            override fun accept(effect: GameEffect) {
                when (effect) {
                    is FetchData -> {
                        gameRepository?.loadGame(arguments?.getLong(ARG_GAME_ID, 0) ?: 0)?.let {
                            eventConsumer.accept(Loaded(it))
                        } ?: requireActivity().onBackPressed() // TODO: Handle error finding game better
                    }
                }
            }

            override fun dispose() {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    // End Mobius functions

    companion object {
        private const val ARG_GAME_ID = "game_id"

        fun newInstance(gameId: Long): GameFragment {
            val fragment = GameFragment()
            val args = Bundle().apply {
                putLong(ARG_GAME_ID, gameId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}