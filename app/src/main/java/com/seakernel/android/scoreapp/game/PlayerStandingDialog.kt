package com.seakernel.android.scoreapp.game

import android.app.Dialog
import android.os.Bundle
import android.system.Os.close
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.database.entities.ScoreEntity
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.repository.PlayerRoundNote
import com.seakernel.android.scoreapp.repository.RoundRepository
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import kotlinx.android.synthetic.main.dialog_player_round.view.*
import kotlinx.android.synthetic.main.holder_player_round_notes.view.*
import kotlinx.android.synthetic.main.holder_player_standing.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerStandingDialog(val gameId: Long) : DialogFragment() {

    private val adapter = PlayerStandingAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_player_round, null, false)
        view.dialogPlayerRoundRecycler.layoutManager = LinearLayoutManager(requireContext())
        view.dialogPlayerRoundRecycler.adapter = adapter

        GlobalScope.launch {
            val game = GameRepository(requireContext()).loadFullGame(gameId)
            val scoreMap = mutableMapOf<Long, Double>()
            game.rounds.forEach { round ->
                round.scores.forEach { score ->
                    scoreMap[score.player.id!!] = (scoreMap[score.player.id] ?: 0.0) + score.value
                }
            }

            val playerScores = scoreMap.map { playerScore ->
                PlayerScore(
                    game.settings.players.find { it.id == playerScore.key }!!.name,
                    playerScore.value
                )
            }.sortedBy { it.score }

            // Set the scores, but we want it reversed (so highest score is first place) unless there is reversed scoring
            adapter.setScores(if (game.settings.reversedScoring) playerScores else playerScores.reversed())
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.playerStandingTitle))
            .setView(view)
            .setNegativeButton(R.string.actionClose, null)
//            .setPositiveButton(R.string.actionSave, null) // TODO: add "New Game" button
            .create()
    }
}

private class PlayerStandingAdapter : RecyclerView.Adapter<PlayerStandingViewHolder>() {
    var playerScores = listOf<PlayerScore>()

    /**
     * @param scores a sorted list of player scores, with position 0 being first place
     */
    fun setScores(scores: List<PlayerScore>) {
        playerScores = scores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PlayerStandingViewHolder(parent)

    override fun getItemCount() = playerScores.size

    override fun onBindViewHolder(holder: PlayerStandingViewHolder, position: Int) {
        holder.onBind(playerScores[position])
    }
}

private class PlayerStandingViewHolder(parent: ViewGroup) :
    BaseViewHolder(parent, R.layout.holder_player_standing) {

    fun onBind(playerScore: PlayerScore) {
        itemView.playerNameHolder.text = playerScore.name
        itemView.playerScoreHolder.text = playerScore.score.toString()
        itemView.playerStandingHolder.text =
            itemView.context.getString(R.string.playerStanding, adapterPosition + 1)
    }
}

private data class PlayerScore(val name: String, val score: Double)