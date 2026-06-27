package com.seakernel.android.scoreapp.game

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.DialogPlayerRoundBinding
import com.seakernel.android.scoreapp.databinding.HolderPlayerRoundNotesBinding
import com.seakernel.android.scoreapp.repository.PlayerRoundNote
import com.seakernel.android.scoreapp.repository.RoundRepository
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerRoundNotesDialog : DialogFragment() {

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_PLAYER = "PLAYER"

        fun newInstance(player: Player, gameId: Long): PlayerRoundNotesDialog {
            val args = Bundle().apply {
                putLong(KEY_GAME_ID, gameId)
                putParcelable(KEY_PLAYER, player)
            }

            val fragment = PlayerRoundNotesDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private val adapter = PlayerRoundNotesAdapter()
    private var _binding: DialogPlayerRoundBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val gameId get() = requireArguments().getLong(KEY_GAME_ID)
    private val player
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(KEY_PLAYER, Player::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(KEY_PLAYER)
        }!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPlayerRoundBinding.inflate(layoutInflater, null, false)

        binding.dialogPlayerRoundRecycler.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
        binding.dialogPlayerRoundRecycler.adapter = adapter
        binding.root.applyWindowInsetsNavigationPadding()

        lifecycleScope.launch(Dispatchers.IO) {
            val roundNotes = RoundRepository(requireContext()).getNotesForPlayer(player, gameId)
            withContext(Dispatchers.Main) {
                adapter.setNotes(roundNotes)
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.playerNotesTitle, player.name))
            .setView(binding.root)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.actionSave, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        val window = dialog?.window ?: return
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val playerRounds = adapter.playerRounds
                RoundRepository(requireContext()).updatePlayerNotes(playerRounds)
                withContext(Dispatchers.Main) {
                    dialog?.dismiss()
                }
            }
        }
    }
}

private class PlayerRoundNotesAdapter : RecyclerView.Adapter<PlayerRoundNotesViewHolder>(),
    NotesUpdatedListener {
    var playerRounds: MutableList<PlayerRoundNote> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setNotes(rounds: List<PlayerRoundNote>) {
        playerRounds.clear()
        playerRounds.addAll(rounds)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PlayerRoundNotesViewHolder(parent, this)

    override fun getItemCount() = playerRounds.size

    override fun onBindViewHolder(holder: PlayerRoundNotesViewHolder, position: Int) {
        holder.onBind(playerRounds[position])
    }

    override fun onNotesUpdated(position: Int, notes: String) {
        val round = playerRounds[position]
        playerRounds[position] = round.copy(score = round.score.copy(metadata = notes))
    }
}

private class PlayerRoundNotesViewHolder(
    parent: ViewGroup,
    private val notesListener: NotesUpdatedListener
) :
    BaseViewHolder<HolderPlayerRoundNotesBinding>(
        HolderPlayerRoundNotesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) {

    init {
        binding.playerRoundValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                notesListener.onNotesUpdated(bindingAdapterPosition, text?.toString() ?: "")
            }
        })
    }

    fun onBind(round: PlayerRoundNote) {
        val notes = round.score.metadata
        // Make the round number 1 based indexed
        binding.playerRoundLabel.text =
            itemView.context.getString(R.string.playerRoundNumberFormat, round.roundNumber + 1)
        binding.playerRoundValue.setText(notes)
        binding.playerRoundValue.setSelection(notes.length)
    }
}

private interface NotesUpdatedListener {
    fun onNotesUpdated(position: Int, notes: String)
}
