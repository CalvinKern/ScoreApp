package com.seakernel.android.scoreapp.playerselect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.FragmentPlayerSelectBinding
import com.seakernel.android.scoreapp.playerselect.CreateModel.Companion.update
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationMargin
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setOptionalIconsVisible
import com.seakernel.android.scoreapp.utility.setStatusSubtitle
import com.seakernel.android.scoreapp.utility.setVisible
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class PlayerSelectFragment : MobiusFragment<CreateModel, PlayerEvent, PlayerEffect>() {

    private var playerRepository: PlayerRepository? = null
    private var gameRepository: GameRepository? = null
    private var addPlayerJob: Job? = null

    private lateinit var toolbarItemClickListener: Toolbar.OnMenuItemClickListener
    private var _binding: FragmentPlayerSelectBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val savesAutomatically get() = arguments?.getBoolean(ARG_SAVES_AUTOMATICALLY) == true
    private val initialPlayerIds get() = arguments?.getLongArray(PLAYER_IDS)?.toList().orEmpty()

    // Whether the picks came back with the screen, to be sent once they load (see connectViews)
    private var sendPicksOnLoad = false

    // Set while the screen is connected to the loop, for the player dialog's result
    private var eventConsumer: Consumer<PlayerEvent>? = null

    init {
        loop = createLoop(::update)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        playerRepository = PlayerRepository(requireContext())
        gameRepository = GameRepository(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        playerRepository = null
        gameRepository = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        controller = MobiusAndroid.controller(
            loop,
            CreateModel.createDefault(
                savedInstanceState?.getLongArray(ARG_SELECTED_IDS)?.toList()
                    ?: arguments?.getLongArray(PLAYER_IDS)?.toList()
            ),
            ::initMobius
        )

        sendPicksOnLoad = savedInstanceState != null
        super.onViewCreated(view, savedInstanceState)

        // Setup views
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.inflateMenu(R.menu.menu_player_select)
        (binding.toolbar.menu.findItem(R.id.actionSearch).actionView as? SearchView)?.queryHint =
            getString(R.string.playerName)
        binding.toolbar.setOptionalIconsVisible()
        binding.playerRecycler.layoutManager = LinearLayoutManager(requireContext())

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsCutout()
        binding.fab.applyWindowInsetsNavigationMargin()
        binding.playerRecycler.applyWindowInsetsNavigationPadding()

        childFragmentManager.setFragmentResultListener(PlayerDialog.REQUEST_PLAYER, viewLifecycleOwner) { _, result ->
            onPlayerDialogResult(result)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addPlayerJob?.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLongArray(ARG_SELECTED_IDS, controller.model.selectedPlayerList.toLongArray())
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.PlayerSelectFragment)
    }

    // Mobius functions

    override fun initMobius(model: CreateModel): First<CreateModel, PlayerEffect> {
        return First.first(model.copy(isLoading = true), setOf(FetchData))
    }

    override fun connectViews(eventConsumer: Consumer<PlayerEvent>): Connection<CreateModel> {
        this.eventConsumer = eventConsumer
        toolbarItemClickListener = Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSave -> eventConsumer.accept(DoneSelectingPlayersClicked)
                R.id.actionSearch -> {
                    (item.actionView as SearchView).setOnQueryTextListener(object :
                        SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean = false

                        override fun onQueryTextChange(newText: String?): Boolean {
                            eventConsumer.accept(PlayerSearchRequest(newText))
                            return true
                        }
                    })
                }
            }
            false
        }

        binding.fab.setOnClickListener {
            eventConsumer.accept(AddPlayerClicked)
        }
        binding.playerEmptyImage.setOnClickListener {
            binding.fab.performClick()
        }
        binding.toolbar.setOnMenuItemClickListener(toolbarItemClickListener)

        return object : Connection<CreateModel> {
            private var lastModel: CreateModel? = null

            override fun accept(model: CreateModel) {
                if (model == lastModel) return

                binding.playerLoading.setVisible(model.isLoading)
                binding.playerEmptyGroup.setVisible(!model.isLoading && model.allPlayers.isEmpty())
                binding.playerRecycler.setVisible(!model.isLoading && model.filteredPlayerList.isNotEmpty())
                binding.playerEmptySearchGroup.setVisible(!model.isLoading && model.allPlayers.isNotEmpty() && model.filteredPlayerList.isEmpty())

                binding.playerRecycler.swapAdapter(
                    PlayerListAdapter(
                        model.filteredPlayerList,
                        model.selectedPlayerList,
                        eventConsumer
                    ), false
                )

                // Picks are sent as they're made, so they're kept however the screen is left (back, or save), and an
                // existing game saves them right away. Renames are sent too, so the game setup shows the new names.
                val previous = lastModel
                val changed = previous != null && !previous.isLoading &&
                    (model.selectedPlayerList != previous.selectedPlayerList || model.allPlayers != previous.allPlayers)
                // Picks that came back with the screen (e.g. after Android closed the app) are sent once they load,
                // in case they were made too late to reach the game setup before the app closed
                val restoredPicksLoaded = sendPicksOnLoad && previous?.isLoading == true && !model.isLoading
                if (changed || restoredPicksLoaded) {
                    val picks = Bundle().apply { putLongArray(KEY_PLAYER_IDS, model.selectedPlayerList.toLongArray()) }
                    setFragmentResult(REQUEST_PLAYERS, picks)
                }
                if (!model.isLoading) sendPicksOnLoad = false

                // An existing game says its picks save on their own, or that none picked keeps its old players
                if (savesAutomatically && !model.isLoading) {
                    val playersNotSaved = model.selectedPlayerList.isEmpty() && initialPlayerIds.isNotEmpty()
                    binding.toolbar.setStatusSubtitle(
                        getString(if (playersNotSaved) R.string.saveStatusPlayersNotSaved else R.string.saveStatusAutomatic),
                        announce = true,
                    )
                }

                lastModel = model
            }

            override fun dispose() {
                // Don't forget to remove listeners when the UI is disconnected
                this@PlayerSelectFragment.eventConsumer = null
                binding.fab.setOnClickListener(null)
                binding.playerEmptyImage.setOnClickListener(null)
                binding.playerRecycler.swapAdapter(null, true)
            }
        }
    }

    override fun effectHandler(eventConsumer: Consumer<PlayerEvent>): Connection<PlayerEffect> {
        return object : Connection<PlayerEffect> {
            private var isDisposed = false

            override fun accept(effect: PlayerEffect) {
                when (effect) {
                    // Effects run in the background, so anything touching the screen moves to the main thread
                    is ShowPlayerDialog -> runWhenStarted {
                        // One at a time, e.g. when the + button is tapped twice quickly
                        if (isDisposed || childFragmentManager.findFragmentByTag(PlayerDialog.TAG) != null) return@runWhenStarted
                        PlayerDialog.newInstance(effect.player, effect.seatedPlayers)
                            .show(childFragmentManager, PlayerDialog.TAG)
                    }
                    is ShowDeletePlayerSnackbar -> {
                        playerRepository?.deleteUser(effect.playerId)
                        eventConsumer.accept(PlayerDeleteSuccessful(effect.playerId))
                        runWhenStarted { showDeletePlayerSnackbar(eventConsumer, effect) { isDisposed } }
                    }
                    is UndoDeletePlayer -> {
                        playerRepository?.undoDelete(effect.playerId)
                        eventConsumer.accept(
                            PlayersLoaded(
                                playerRepository?.loadAllUsers() ?: listOf()
                            )
                        )
                    }
                    is FetchData -> {
                        eventConsumer.accept(
                            PlayersLoaded(
                                playerRepository?.loadAllUsers() ?: listOf()
                            )
                        )
                    }
                    // The picked players were already sent back as they changed (see connectViews)
                    is DoneSelectingPlayers -> runWhenStarted { parentFragmentManager.popBackStack() }
                }
            }

            override fun dispose() {
                isDisposed = true
            }
        }
    }

    // End Mobius functions

    private fun showDeletePlayerSnackbar(
        eventConsumer: Consumer<PlayerEvent>,
        effect: ShowDeletePlayerSnackbar,
        isDisposed: () -> Boolean
    ) {
        Snackbar.make(
            requireView(),
            getString(R.string.deleteSuccessful, effect.playerName ?: ""),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.undo) {
                if (!isDisposed()) {
                    eventConsumer.accept(PlayerDeleteUndo(effect.playerId, effect.playerSelected))
                }
            }
            .show()
    }

    /** Saves the player from the player dialog (or deletes them), see [PlayerDialog] */
    private fun onPlayerDialogResult(result: Bundle) {
        val player = BundleCompat.getParcelable(result, PlayerDialog.KEY_PLAYER, Player::class.java) ?: return
        if (result.getBoolean(PlayerDialog.KEY_DELETE)) {
            player.id?.let { eventConsumer?.accept(PlayerDeleteClicked(it)) }
            return
        }

        addPlayerJob = lifecycleScope.launch(Dispatchers.IO) {
            val saved = playerRepository?.addOrUpdateUser(player.id, player.name, player.color) ?: return@launch
            withContext(Dispatchers.Main) {
                eventConsumer?.accept(PlayerSaved(saved)) // Gone when the screen closed first
            }
        }
    }

    companion object {
        private const val ARG_SELECTED_IDS = "selected_ids"
        private const val PLAYER_IDS = "game_id"
        private const val ARG_SAVES_AUTOMATICALLY = "saves_automatically"

        /** The picked players, sent with [KEY_PLAYER_IDS] (in seat order) whenever they change */
        const val REQUEST_PLAYERS = "PLAYERS"
        const val KEY_PLAYER_IDS = "player_ids"

        /** @param savesAutomatically whether the picks save to the game as they're made, which the screen says */
        fun newInstance(playerIds: List<Long>, savesAutomatically: Boolean = false): PlayerSelectFragment {
            return PlayerSelectFragment().apply {
                arguments = Bundle().apply {
                    putLongArray(PLAYER_IDS, playerIds.toLongArray())
                    putBoolean(ARG_SAVES_AUTOMATICALLY, savesAutomatically)
                }
            }
        }
    }
}
