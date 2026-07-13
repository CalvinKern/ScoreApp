package com.seakernel.android.scoreapp.playerselect

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewGroupCompat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class PlayerSelectFragment : MobiusFragment<CreateModel, PlayerEvent, PlayerEffect>() {

    interface PlayerSelectListener {
        fun onPlayersSelected(playerIds: List<Long>)
    }

    private var playerRepository: PlayerRepository? = null
    private var gameRepository: GameRepository? = null
    private var listener: PlayerSelectListener? = null
    private var addPlayerJob: Job? = null

    private lateinit var toolbarItemClickListener: Toolbar.OnMenuItemClickListener
    private var _binding: FragmentPlayerSelectBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    init {
        loop = Mobius.loop(::update, ::effectHandler)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        playerRepository = PlayerRepository(requireContext())
        gameRepository = GameRepository(requireContext())

        listener = context as? PlayerSelectListener
    }

    override fun onDetach() {
        super.onDetach()
        playerRepository = null
        gameRepository = null
        listener = null
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
        binding.playerRecycler.layoutManager = LinearLayoutManager(requireContext())

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.toolbar.applyWindowInsetsCutout()
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
                    is ShowPlayerNameDialog -> {
                        view?.post {
                            if (!isDisposed) {
                                showPlayerNameDialog(eventConsumer, effect) { isDisposed }
                            }
                        }
                    }
                    is ShowDeletePlayerSnackbar -> {
                        playerRepository?.deleteUser(effect.playerId)
                        eventConsumer.accept(PlayerDeleteSuccessful(effect.playerId))
                        showDeletePlayerSnackbar(eventConsumer, effect) { isDisposed }
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
                    is DoneSelectingPlayers -> listener?.onPlayersSelected(effect.playerIds)
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
            .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary))
            .show()
    }

    private fun showPlayerNameDialog(
        eventConsumer: Consumer<PlayerEvent>,
        effect: ShowPlayerNameDialog,
        isDisposed: () -> Boolean
    ) {
        var name = ""
        val binding =
            DialogPlayerNameBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (effect.playerId == null) R.string.playerCreateTitle else R.string.playerName)
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (name.isEmpty()) return@setPositiveButton
                logEvent(if (effect.playerId == null) AnalyticsConstants.Event.PLAYER_CREATE else AnalyticsConstants.Event.PLAYER_RENAMED) {
                    putString(AnalyticsConstants.Param.ITEM_NAME, name)
                }
                addPlayerJob = lifecycleScope.launch(Dispatchers.IO) {
                    playerRepository?.addOrUpdateUser(effect.playerId, name)?.let { player ->
                        if (isDisposed()) return@let
                        withContext(Dispatchers.Main) {
                            if (isDisposed()) return@withContext // Double check we're not disposed
                            eventConsumer.accept(
                                PlayerNameChanged(
                                    player.id!!,
                                    player.name
                                )
                            )
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .also {
                if (effect.playerId == null) return@also

                it.setNeutralButton(R.string.delete) { _, _ ->
                    logEvent(AnalyticsConstants.Event.PLAYER_DELETED)
                    if (!isDisposed()) {
                        eventConsumer.accept(PlayerDeleteClicked(effect.playerId))
                    }
                }
            }
            .create()

        binding.playerNameEdit.apply {
            setText(effect.playerName)
            setSelection(effect.playerName.length)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    name = p0?.toString() ?: ""
                    dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = name.isNotEmpty()
                }
            })
        }

        // Disable "ok" button until there is text in the dialog
        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = effect.playerName.isNotEmpty()
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    companion object {
        private const val ARG_SELECTED_IDS = "selected_ids"
        private const val PLAYER_IDS = "game_id"

        fun newInstance(playerIds: List<Long>): PlayerSelectFragment {
            return PlayerSelectFragment().apply {
                arguments = Bundle().apply { putLongArray(PLAYER_IDS, playerIds.toLongArray()) }
            }
        }
    }
}
