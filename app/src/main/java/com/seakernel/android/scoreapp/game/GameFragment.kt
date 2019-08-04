package com.seakernel.android.scoreapp.game

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
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

    interface GameListener {
        fun onGameSettingsSelected(gameId: Long)
    }

    override val layoutId = R.layout.fragment_game

    private var listener: GameListener? = null
    private var gameRepository: GameRepository? = null
    private var roundRepository: RoundRepository? = null
    private var eventConsumer: Consumer<GameEvent>? = null

    init {
        loop = Mobius.loop(GameModel.Companion::update, ::effectHandler).init(::initMobius)
        controller = MobiusAndroid.controller(loop, GameModel.createDefault())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? GameListener
        gameRepository = GameRepository(requireContext())
        roundRepository = RoundRepository(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        gameRepository = null
        roundRepository = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // TODO: Restore state
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        toolbar.inflateMenu(R.menu.menu_game)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionEdit -> {
                    arguments?.getLong(ARG_GAME_ID)?.let { gameId ->
                        listener?.onGameSettingsSelected(gameId)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbar.setNavigationOnClickListener(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // TODO: Store state
    }

    private fun setupHeaderAndFooter(players: List<Player>) {
        if ((totalsRow.layoutManager as? GridLayoutManager)?.spanCount != players.size) {
            totalsRow.layoutManager = GridLayoutManager(requireContext(), players.size)
        }
        if ((nameRow.layoutManager as? GridLayoutManager)?.spanCount != players.size) {
            nameRow.layoutManager = GridLayoutManager(requireContext(), players.size)
        }

        nameRow.swapAdapter(PlayersAdapter(players), false)
    }

    override fun onResume() {
        super.onResume()
        eventConsumer?.accept(GameEvent.RequestLoad)
    }

    // Mobius functions

    override fun initMobius(model: GameModel): First<GameModel, GameEffect> {
        return First.first(model, setOf(GameEffect.FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<GameEvent>): Connection<GameModel> {
        this.eventConsumer = eventConsumer

        return object : Connection<GameModel> {
            override fun accept(model: GameModel) {
                toolbar.title = model.settings.name

                var manager = scoreRows.layoutManager as? GridLayoutManager
                val oldSpanCount = manager?.spanCount
                if (model.settings.players.isNotEmpty()) {
                    setupHeaderAndFooter(model.settings.players)

                    val spanCount = model.settings.players.size
                    if (oldSpanCount != spanCount) {
                        // Reset the layout manager if the number of players has changed
                        manager = GridLayoutManager(requireContext(), spanCount)
                        scoreRows.layoutManager = manager
                    }
                    manager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int) =
                            if (position == (model.rounds.size * spanCount)) spanCount else 1
                    }
                }

                // If count is not empty (account for empty with add round row), and new count is greater (added a row)
                val oldCount = scoreRows.adapter?.itemCount ?: 0
                scoreRows.swapAdapter(GameScoreAdapter(model.settings.hasDealer, model.rounds, eventConsumer), false)
                val newCount = scoreRows.adapter!!.itemCount
                @Suppress("ConvertTwoComparisonsToRangeCheck") // Seems much less efficient than a range and doesn't help readability in this situation
                if (oldCount > 1 && oldCount < newCount && oldSpanCount == model.settings.players.size) {
                    // When a new round is being inserted, scroll to the bottom and request focus to remove focus from the text view.
                    // This allows us to record a score when a new round is being inserted, without the user having to press the 'next' button.
                    scoreRows.scrollToPosition(newCount - 1)
                    scoreRows.focusedChild?.clearFocus()
                }

                totalsRow.swapAdapter(TotalsAdapter(model.settings.reversedScoring, model.rounds), false)
            }

            override fun dispose() {}
        }
    }

    override fun effectHandler(eventConsumer: Consumer<GameEvent>): Connection<GameEffect> {
        return object : Connection<GameEffect> {
            override fun accept(effect: GameEffect) {
                when (effect) {
                    is GameEffect.FetchData -> {
                        gameRepository?.loadFullGame(arguments?.getLong(ARG_GAME_ID, 0) ?: 0)?.let {
                            eventConsumer.accept(GameEvent.Loaded(it))
                        } ?: requireActivity().onBackPressed() // TODO: Handle error finding game better
                    }
                    is GameEffect.SaveRound -> {
                        roundRepository?.addOrUpdateRound(effect.gameId, effect.round)?.let {
                            eventConsumer.accept(GameEvent.RoundSaved(it))
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