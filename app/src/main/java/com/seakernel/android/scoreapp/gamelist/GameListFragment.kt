package com.seakernel.android.scoreapp.gamelist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewGroupCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.FragmentGameListBinding
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationMargin
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setVisible
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.seakernel.android.scoreapp.utility.setOptionalIconsVisible

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameListFragment : MobiusFragment<ListModel, ListEvent, ListEffect>() {

    interface GameListListener {
        fun onShowGameScreen(gameId: Long)
        fun onShowCreateGameScreen()
        fun onShowSettingsScreen()
    }

    private var gameRepository: GameRepository? = null
    private var listener: GameListListener? = null
    private var _binding: FragmentGameListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    init {
        loop = Mobius.loop(ListModel.Companion::update, ::effectHandler)
        controller = MobiusAndroid.controller(loop, ListModel.createDefault(), ::initMobius)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup views
        binding.gameRecycler.layoutManager = LinearLayoutManager(requireContext())
        activity?.setTitle(R.string.gameListTitle)

        // Setup Toolbar
        binding.toolbar.inflateMenu(R.menu.menu_game_list)
        binding.toolbar.setOptionalIconsVisible()
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionSettings -> {
                    listener?.onShowSettingsScreen()
                    true
                }
                else -> false
            }
        }

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsCutout()
        binding.fab.applyWindowInsetsNavigationMargin()
        binding.gameRecycler.applyWindowInsetsNavigationPadding()
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.GameListFragment)
    }

    // Mobius functions

    override fun initMobius(model: ListModel): First<ListModel, ListEffect> {
        return First.first(model.copy(isLoading = true), setOf(ListEffect.FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<ListEvent>): Connection<ListModel> {
        // Send events to the consumer when the button is pressed
        binding.fab.setOnClickListener {
            logEvent(AnalyticsConstants.Event.GAME_CREATED)
            eventConsumer.accept(ListEvent.AddGameClicked)
        }

        binding.gameListEmptyImage.setOnClickListener {
            binding.fab.performClick()
        }

        return object : Connection<ListModel> {
            override fun accept(model: ListModel) {
                binding.gameListLoading.setVisible(model.isLoading)
                binding.gameRecycler.setVisible(!model.isLoading && model.gameList.isNotEmpty())
                binding.gameListEmptyGroup.setVisible(!model.isLoading && model.gameList.isEmpty())

                binding.gameRecycler.swapAdapter(
                    GameListAdapter(model.gameList, eventConsumer),
                    true
                )
            }

            override fun dispose() {
                // Don't forget to remove listeners when the UI is disconnected
                binding.fab.setOnClickListener(null)
                binding.gameListEmptyImage.setOnClickListener(null)
                binding.gameRecycler.swapAdapter(null, true)
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
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setMessage(R.string.deleteGameConfirmation)
                .setPositiveButton(R.string.delete) { _, _ ->
                    deleteGameAsync(eventConsumer, gameId)
                }
                .setNegativeButton(android.R.string.cancel, null)
            builder.create().show()
        }
    }

    private fun deleteGameAsync(eventConsumer: Consumer<ListEvent>, gameId: Long) {
        logEvent(AnalyticsConstants.Event.GAME_DELETED)
        lifecycleScope.launch(Dispatchers.IO) {
            val deleted = gameRepository?.deleteGame(gameId)
            withContext(Dispatchers.Main) {
                if (deleted == true) {
                    eventConsumer.accept(ListEvent.GameDeleteSuccessful(gameId))
                } else {
                    // TODO: Show error, shouldn't happen, but why not catch it?
                    Toast.makeText(requireContext(), R.string.delete, Toast.LENGTH_SHORT).show()
                }
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
