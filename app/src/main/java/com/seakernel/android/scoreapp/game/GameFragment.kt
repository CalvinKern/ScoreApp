package com.seakernel.android.scoreapp.game

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.RoundRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game.*

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameFragment : MobiusFragment<GameModel, GameEvent, GameEffect>() {

    override val layoutId = R.layout.fragment_game

    private var gameRepository: GameRepository? = null
    private var roundRepository: RoundRepository? = null

    init {
        loop = Mobius.loop(GameModel.Companion::update, ::effectHandler).init(::initMobius)
        controller = MobiusAndroid.controller(loop, GameModel.createDefault())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gameRepository = GameRepository(requireContext())
        roundRepository = RoundRepository(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        gameRepository = null
        roundRepository = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // TODO: Restore state
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbar.setNavigationOnClickListener(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // TODO: Store state
    }

    private fun setupHeaderAndFooter(players: List<Player>) {
        totalsRow.layoutManager = GridLayoutManager(requireContext(), players.size)
        nameRow.layoutManager = GridLayoutManager(requireContext(), players.size)

        // Only set the name adapter, that will never be changed. Scores need to update every score update
        nameRow.adapter = PlayersAdapter(players)

        totalsRow.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        nameRow.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }

    // Mobius functions

    override fun initMobius(model: GameModel): First<GameModel, GameEffect> {
        return First.first(model, setOf(FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<GameEvent>): Connection<GameModel> {
        return object : Connection<GameModel> {
            override fun accept(model: GameModel) {
                toolbar.title = model.settings.name

                if (scoreRows.layoutManager == null && model.settings.players.isNotEmpty()) {
                    scoreRows.layoutManager = GridLayoutManager(requireContext(), model.settings.players.size)
                    scoreRows.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                        if (bottom < oldBottom) scoreRows.layoutManager?.smoothScrollToPosition(scoreRows, null, scoreRows.adapter?.itemCount ?: 0)
                    }
                    setupHeaderAndFooter(model.settings.players)
                }
                scoreRows.swapAdapter(GameScoreAdapter(model.rounds, eventConsumer), false)
                totalsRow.swapAdapter(TotalsAdapter(model.rounds), false)
            }

            override fun dispose() {}
        }
    }

    override fun effectHandler(eventConsumer: Consumer<GameEvent>): Connection<GameEffect> {
        return object : Connection<GameEffect> {
            override fun accept(effect: GameEffect) {
                when (effect) {
                    is FetchData -> {
                        gameRepository?.loadFullGame(arguments?.getLong(ARG_GAME_ID, 0) ?: 0)?.let {
                            eventConsumer.accept(Loaded(it))
                        } ?: requireActivity().onBackPressed() // TODO: Handle error finding game better
                    }
                    is SaveRound -> {
                        roundRepository?.addOrUpdateRound(effect.gameId, effect.round)?.let {
                            eventConsumer.accept(RoundSaved(it))
                        }
                    }
                }.hashCode() // Exhaustive call
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