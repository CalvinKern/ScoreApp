package com.seakernel.android.scoreapp.playerselect

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.ViewGroupCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.DialogPlayerNameBinding
import com.seakernel.android.scoreapp.databinding.FragmentPlayerSelectBinding
import com.seakernel.android.scoreapp.playerselect.CreateModel.Companion.update
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRepository
import com.seakernel.android.scoreapp.ui.MobiusFragment
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.PlayerColors
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationMargin
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logEvent
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.setOptionalIconsVisible
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

                // Picks save on their own, so they're kept however the screen is left (back, or save). Results only
                // reach the game setup once it shows again, so it gets the last picks and its players' new names.
                val previous = lastModel
                if (previous != null && !previous.isLoading &&
                    (model.selectedPlayerList != previous.selectedPlayerList || model.allPlayers != previous.allPlayers)
                ) {
                    setFragmentResult(REQUEST_PLAYERS, bundleOf(KEY_PLAYER_IDS to model.selectedPlayerList.toLongArray()))
                }

                lastModel = model
            }

            override fun dispose() {
                // Don't forget to remove listeners when the UI is disconnected
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
                        if (!isDisposed) showPlayerDialog(eventConsumer, effect) { isDisposed }
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

    private fun showPlayerDialog(
        eventConsumer: Consumer<PlayerEvent>,
        effect: ShowPlayerDialog,
        isDisposed: () -> Boolean
    ) {
        val player = effect.player
        val binding =
            DialogPlayerNameBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        val name = { binding.playerNameEdit.text?.toString().orEmpty() }
        lateinit var colorPicker: PlayerColorPicker
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (player.id == null) R.string.playerCreateTitle else R.string.playerName)
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = name()
                if (newName.isEmpty() || !colorPicker.isValid) return@setPositiveButton
                // Player names are personal data, so only the event is logged
                logEvent(if (player.id == null) AnalyticsConstants.Event.PLAYER_CREATE else AnalyticsConstants.Event.PLAYER_RENAMED)
                addPlayerJob = lifecycleScope.launch(Dispatchers.IO) {
                    playerRepository?.addOrUpdateUser(player.id, newName, colorPicker.color)?.let { saved ->
                        if (isDisposed()) return@let
                        withContext(Dispatchers.Main) {
                            if (isDisposed()) return@withContext // Double check we're not disposed
                            eventConsumer.accept(PlayerSaved(saved))
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .also {
                val playerId = player.id ?: return@also

                it.setNeutralButton(R.string.delete) { _, _ ->
                    logEvent(AnalyticsConstants.Event.PLAYER_DELETED)
                    if (!isDisposed()) {
                        eventConsumer.accept(PlayerDeleteClicked(playerId))
                    }
                }
            }
            .create()

        // OK needs a name, and a color that can be saved
        val updateOk = { dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = name().isNotEmpty() && colorPicker.isValid }
        val seatColor = effect.seatedPlayers.let { PlayerColors.forSeat(requireContext(), it, it.indexOf(player)) }
        colorPicker = PlayerColorPicker(binding, player.color, seatColor, updateOk)
        binding.playerNameEdit.apply {
            setText(player.name)
            setSelection(player.name.length)
            doAfterTextChanged { updateOk() }
        }
        dialog.setOnShowListener { updateOk() }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    companion object {
        private const val ARG_SELECTED_IDS = "selected_ids"
        private const val PLAYER_IDS = "game_id"

        /** The picked players, sent with [KEY_PLAYER_IDS] (in seat order) whenever they change */
        const val REQUEST_PLAYERS = "PLAYERS"
        const val KEY_PLAYER_IDS = "player_ids"

        fun newInstance(playerIds: List<Long>): PlayerSelectFragment {
            return PlayerSelectFragment().apply {
                arguments = Bundle().apply { putLongArray(PLAYER_IDS, playerIds.toLongArray()) }
            }
        }
    }
}
