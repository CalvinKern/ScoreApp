package com.seakernel.android.scoreapp.game

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.database.ScoreEntity
import com.seakernel.android.scoreapp.repository.RoundPlayerNote
import com.seakernel.android.scoreapp.repository.RoundRepository
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import kotlinx.android.synthetic.main.dialog_player_round.view.*
import kotlinx.android.synthetic.main.holder_player_round_notes.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerRoundNotesDialog(private val player: Player, private val gameId: Long) : DialogFragment() {

    private val adapter = PlayerRoundNotesAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_player_round, null, false)
        view.dialogPlayerRoundRecycler.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
        view.dialogPlayerRoundRecycler.adapter = adapter

        GlobalScope.launch {
            val roundNotes = RoundRepository(requireContext()).getNotesForPlayer(player, gameId)
            adapter.setNotes(roundNotes)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.playerNotesTitle, player.name))
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.actionSave, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            GlobalScope.launch {
                val playerRounds = adapter.playerRounds
                RoundRepository(requireContext()).updateScores(*playerRounds.map { round ->
                    ScoreEntity(round.score.id, player.id!!, round.roundId, round.score.value, round.score.metadata)
//                    round.scores.map {
//                        ScoreEntity(it.id, player.id!!, round.id!!, it.value, it.metadata)
//                    }
                }.toTypedArray())
                dialog?.dismiss()
            }
        }
    }
}

private class PlayerRoundNotesAdapter: RecyclerView.Adapter<PlayerRoundNotesViewHolder>(),
    NotesUpdatedListener {
    var playerRounds: MutableList<RoundPlayerNote> = ArrayList()

    init {
//        playerRounds = rounds.map { round ->
//            round.copy(scores = round.scores.filter { score -> score.player.id == playerId })
//        }.toMutableList()
    }

    fun setNotes(rounds: List<RoundPlayerNote>) {
        playerRounds.clear()
        playerRounds.addAll(rounds)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =  PlayerRoundNotesViewHolder(parent, this)

    override fun getItemCount() = playerRounds.size

    override fun onBindViewHolder(holder: PlayerRoundNotesViewHolder, position: Int) {
        holder.onBind(playerRounds[position])
    }

    override fun onNotesUpdated(position: Int, notes: String) {
        val round = playerRounds[position]
        playerRounds[position] = round.copy(score = round.score.copy(metadata = notes))
    }
}

private class PlayerRoundNotesViewHolder(parent: ViewGroup, private val notesListener: NotesUpdatedListener) : BaseViewHolder(parent, R.layout.holder_player_round_notes) {
    init {
        itemView.playerRoundValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                notesListener.onNotesUpdated(adapterPosition, text?.toString() ?: "")
            }
        })
    }

    fun onBind(round: RoundPlayerNote) {
        val notes = round.score.metadata
        // Make the round number human readable
        itemView.playerRoundLabel.text = itemView.context.getString(R.string.playerRoundNumberFormat, round.roundNumber + 1)
        itemView.playerRoundValue.setText(notes)
        itemView.playerRoundValue.setSelection(notes.length)
    }
}

private interface NotesUpdatedListener {
    fun onNotesUpdated(position: Int, notes: String)
}
