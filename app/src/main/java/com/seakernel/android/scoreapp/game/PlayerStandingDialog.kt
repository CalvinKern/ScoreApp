package com.seakernel.android.scoreapp.game

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.DialogPlayerRoundBinding
import com.seakernel.android.scoreapp.databinding.HolderPlayerStandingBinding
import com.seakernel.android.scoreapp.game.classic.GameFragment
import com.seakernel.android.scoreapp.repository.GameRepository
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class PlayerStandingDialog(val gameId: Long) : DialogFragment() {

    private val adapter = PlayerStandingAdapter()
    private var _binding: DialogPlayerRoundBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPlayerRoundBinding.inflate(layoutInflater, null, false)

        binding.dialogPlayerRoundRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.dialogPlayerRoundRecycler.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.playerStandingTitle))
            .setView(binding.root)
            .setNegativeButton(R.string.actionClose, null)
            .setPositiveButton(R.string.gameCreateTitle) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    GameFragment.REQUEST_NEW_GAME,
                    Bundle().apply { },
                )
            }
            .create()

        lifecycleScope.launch(Dispatchers.IO) {
            val game = GameRepository(requireContext()).loadFullGame(gameId)
            val scoreMap = mutableMapOf<Long, Double>()
            game.rounds.forEach { round ->
                round.scores.forEach { score ->
                    scoreMap[score.player.id!!] = (scoreMap[score.player.id] ?: 0.0) + score.value
                }
            }

            var position = 1
            val playerScores = scoreMap
                .map { playerScore ->
                    PlayerScore(
                        game.settings.players.find { it.id == playerScore.key }!!.name,
                        playerScore.value,
                        -1
                    )
                }
                .sortedBy { it.score }
                // Set the scores, but we want it reversed (so highest score is first place) unless there is reversed scoring
                .let { if (game.settings.reversedScoring) it else it.reversed() }
                .let {
                    it.mapIndexed { index, playerScore ->
                        // Update position if we have a new score
                        if (index == 0 || playerScore.score != it[index - 1].score) {
                            position = index + 1
                        }
                        PlayerScore(playerScore.name, playerScore.score, position)
                    }
                }

            withContext(Dispatchers.Main) {
                adapter.setScores(playerScores)
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class PlayerStandingAdapter : RecyclerView.Adapter<PlayerStandingViewHolder>() {
    var playerScores = mutableListOf<PlayerScore>()

    /**
     * @param scores a sorted list of player scores, with position 0 being first place
     */
    fun setScores(scores: List<PlayerScore>) {
        playerScores.clear()
        playerScores.addAll(scores)
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
    BaseViewHolder<HolderPlayerStandingBinding>(
        HolderPlayerStandingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) {

    fun onBind(playerScore: PlayerScore) {
        binding.playerNameHolder.text = playerScore.name
        binding.playerScoreHolder.text = DecimalFormat("##.###").format(playerScore.score)
        binding.playerStandingHolder.text =
            itemView.context.getString(R.string.playerStanding, playerScore.position)
    }
}

private data class PlayerScore(val name: String, val score: Double, val position: Int)
