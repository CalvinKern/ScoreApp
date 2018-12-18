package com.seakernel.android.scoreapp.gamelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.gamelist.ListModel.Companion.update
import com.spotify.mobius.Connection
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game_list.*

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameListFragment : Fragment() {

    private val loop = Mobius.loop(::update, ::effectHandler)
    private val controller = MobiusAndroid.controller(loop, ListModel.createDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_list, container, false)
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
        gameRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Setup Mobius
        controller.connect(::connectViews)
    }

    // Mobius functions

    private fun connectViews(eventConsumer: Consumer<ListEvent>): Connection<ListModel> {
        // Send events to the consumer when the button is pressed
        fab.setOnClickListener { _ ->
            // TODO: Show screen to add a new game
            eventConsumer.accept(AddGameClicked)
        }

        return object : Connection<ListModel> {
            override fun accept(model: ListModel) {
                // This will be called whenever there is a new model
                // TODO: Update adapter with list and eventConsumer
            }

            override fun dispose() {
                // Don't forget to remove listeners when the UI is disconnected
                fab.setOnClickListener(null)
            }
        }
    }

    private fun effectHandler(eventConsumer: Consumer<ListEvent>): Connection<ListEffect> {
        return object : Connection<ListEffect> {
            override fun accept(value: ListEffect) {
                when (value) {
                    is ShowNewGameScreen -> {}
                    is ShowGameScreen -> {}
                    is ShowGameRowDialog -> {}
                    is ShowDeleteSnackbar -> {}
                    is FetchData -> {}
                }
            }

            override fun dispose() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    // End Mobius functions

    companion object {
        fun newInstance() : GameListFragment {
            return GameListFragment()
        }
    }
}