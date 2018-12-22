package com.seakernel.android.scoreapp.gamelist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.fragment_game_list.*
import kotlinx.android.synthetic.main.holder_game_list.view.*

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
        return First.first(model, setOf(FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<ListEvent>): Connection<ListModel> {
        // Send events to the consumer when the button is pressed
        fab.setOnClickListener { _ ->
            eventConsumer.accept(AddGameClicked)
        }

        return object : Connection<ListModel> {
            override fun accept(model: ListModel) {
                // This will be called whenever there is a new model
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
                    is ShowCreateGameScreen -> listener?.onShowCreateGameScreen()
                    is ShowGameScreen -> listener?.onShowGameScreen(effect.gameId)
                    is ShowGameRowDialog -> {}
                    is ShowDeleteSnackbar -> {}
                    is FetchData -> {
                        eventConsumer.accept(Loaded(gameRepository?.loadAllGames() ?: listOf()))
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
        fun newInstance() : GameListFragment {
            return GameListFragment()
        }
    }
}

private class GameListAdapter(private val gameList: List<Game>, private val eventConsumer: Consumer<ListEvent>) : RecyclerView.Adapter<GameListViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(GameListViewHolder.RESOURCE_ID, parent, false)
        return GameListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gameList.count()
    }

    override fun onBindViewHolder(holder: GameListViewHolder, position: Int) {
        holder.bind(gameList[position], eventConsumer)
    }

    override fun getItemId(position: Int): Long {
        return gameList[position].id
    }
}

private class GameListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val nameHolder: TextView by lazy { itemView.gameNameHolder }
    val dateHolder: TextView by lazy { itemView.gameDateHolder }
    val playersHolder: TextView by lazy { itemView.gamePlayersHolder }

    fun bind(game: Game, eventConsumer: Consumer<ListEvent>) {
        nameHolder.text = game.name
        dateHolder.text = game.lastPlayedAt
        playersHolder.text = itemView.context.getString(R.string.playersHolder, game.players.size)

        itemView.setOnClickListener { eventConsumer.accept(GameRowClicked(game.id)) }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_game_list
    }
}