package com.seakernel.android.scoreapp.gamelist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
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

        // Setup Toolbar
        toolbar.inflateMenu(R.menu.menu_game_list)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionRate -> {
                    rateApp()
                    true
                }
                R.id.actionChangelog -> {
                    openChangelog()
                    true
                }
                else -> false
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.gameListTitle)
    }

    override fun onResume() {
        super.onResume()
        logScreenView(GameListFragment::class.java.name)
    }

    // Mobius functions

    override fun initMobius(model: ListModel): First<ListModel, ListEffect> {
        return First.first(model.copy(isLoading = true), setOf(ListEffect.FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<ListEvent>): Connection<ListModel> {
        // Send events to the consumer when the button is pressed
        fab.setOnClickListener {
            logEvent(AnalyticsConstants.Event.CREATE_GAME)
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
                        eventConsumer.accept(
                            ListEvent.Loaded(
                                gameRepository?.loadAllGames() ?: listOf()
                            )
                        )
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
        logEvent(AnalyticsConstants.Event.DELETE_GAME)
        GlobalScope.launch {
            if (gameRepository?.deleteGame(gameId) == true) {
                eventConsumer.accept(ListEvent.GameDeleteSuccessful(gameId))
            } else {
                // TODO: Show error, shouldn't happen, but why not catch it?
                Toast.makeText(requireContext(), R.string.delete, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rateApp() {
        logEvent(AnalyticsConstants.Event.SHOW_RATING_DIALOG)
        val manager = ReviewManagerFactory.create(requireContext())
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = request.result
                val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                }
            } else {
                // There was some problem, continue regardless of the result.
                openPlayStore()
                logEvent(AnalyticsConstants.Event.FAILED_RATING_DIALOG) {
                    putString(AnalyticsConstants.Param.MESSAGE, request.exception?.message)
                }
            }
        }
    }

    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=com.seakernel.scorepad")
            setPackage("com.android.vending")
        }
        startActivity(intent)
    }

    private fun openChangelog() {
        logEvent(AnalyticsConstants.Event.SHOW_CHANGELOG)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/CalvinKern/ScoreApp/releases")
        }
        startActivity(intent)
    }

    // End Mobius functions

    companion object {
        fun newInstance(): GameListFragment {
            return GameListFragment()
        }
    }
}