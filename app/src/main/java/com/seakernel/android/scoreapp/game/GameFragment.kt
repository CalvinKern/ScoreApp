package com.seakernel.android.scoreapp.game

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.repository.GameRepository
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameFragment : Fragment() {

    private val loop = Mobius.loop(GameModel.Companion::update, ::effectHandler).init(::initMobius)
    private val controller: MobiusLoop.Controller<GameModel, GameEvent> = MobiusAndroid.controller(loop, GameModel.createDefault())

    private var gameRepository: GameRepository? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gameRepository = GameRepository(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        gameRepository = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game, container, false) // TODO: Restore state
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup views

        // Setup Mobius
        controller.connect(::connectViews)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // TODO: Store state
    }

    // Mobius functions

    private fun initMobius(model: GameModel): First<GameModel, GameEffect> {
        return First.first(model, setOf(FetchData))
    }

    private fun connectViews(eventConsumer: Consumer<GameEvent>): Connection<GameModel> {
        return object : Connection<GameModel> {
            override fun accept(model: GameModel) {
                GlobalScope.launch(Dispatchers.Main) {
                    gameName.text = model.game.name
                    gamePlayers.text = model.game.players.joinToString { it.name }
                }
            }

            override fun dispose() {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }

    private fun effectHandler(consumer: Consumer<GameEvent>): Connection<GameEffect> {
        return object : Connection<GameEffect> {
            override fun accept(effect: GameEffect) {
                when (effect) {
                    is FetchData -> {
                        gameRepository?.loadGame(arguments?.getLong(ARG_GAME_ID, 0) ?: 0)?.let {
                            consumer.accept(Loaded(it))
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
        const val FRAGMENT_TAG = "game_fragment"
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