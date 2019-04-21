package com.seakernel.android.scoreapp.game

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.holder_score_row_data.view.*
import kotlinx.android.synthetic.main.holder_score_row_header.view.*

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameScoreAdapter(private val rounds: List<Round>, private val eventConsumer: Consumer<GameEvent>? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int = when {
        position < playerCount() -> PlayerViewHolder.RESOURCE_ID
        else -> ScoreViewHolder.RESOURCE_ID
    }

    override fun getItemCount(): Int = (rounds.count() * playerCount()) + (playerCount() * 2)

    override fun getItemId(position: Int): Long =
        when {
            position < playerCount() -> position.toLong() // player header
            position >= (rounds.count() * playerCount()) + playerCount() -> 0 - position.toLong() // player total
            else -> rounds[toRoundIndex(position)].scores[toScoreIndex(position)].id + (playerCount() * 2)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            PlayerViewHolder.RESOURCE_ID -> PlayerViewHolder(LayoutInflater.from(parent.context).inflate(PlayerViewHolder.RESOURCE_ID, parent, false))
            ScoreViewHolder.RESOURCE_ID -> ScoreViewHolder(LayoutInflater.from(parent.context).inflate(ScoreViewHolder.RESOURCE_ID, parent, false))
            else -> throw IllegalStateException("No view holder for provided view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PlayerViewHolder -> holder.bind(rounds.first().scores[position].player)
            is ScoreViewHolder -> {
                val roundIndex = toRoundIndex(position)
                if (roundIndex >= rounds.size) {
                    holder.clearBind()
                } else {
                    val round = rounds[roundIndex]
                    holder.bind(rounds, round, round.scores[toScoreIndex(position)], eventConsumer)
                }
            }
        }
    }

    // Helper functions

    private fun playerCount() = rounds.first().scores.size

    private fun toScoreIndex(position: Int) = (position - playerCount()) % playerCount()

    private fun toRoundIndex(position: Int): Int = (position - playerCount()) / playerCount()
}

class PlayersAdapter(private val players: List<Player>) : RecyclerView.Adapter<PlayerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder =
        PlayerViewHolder(LayoutInflater.from(parent.context).inflate(PlayerViewHolder.RESOURCE_ID, parent, false))

    override fun getItemCount(): Int {
        return players.count()
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }
}

class TotalsAdapter(private val rounds: List<Round>) : RecyclerView.Adapter<ScoreViewHolder>() {
    private val leadPlayerIds: ArrayList<Long> = arrayListOf()
    private val totalsMap: HashMap<Long, Int> = HashMap(rounds.size) // PlayerID to total

    init {
        setHasStableIds(true)

        rounds.forEach { round ->
            round.scores.forEach { score ->
                totalsMap[score.player.id] = (totalsMap[score.player.id] ?: 0) + score.value
            }
        }

        var leadScore = 0
        totalsMap.forEach {
            if (leadScore > it.value) return@forEach
            if (leadScore < it.value) leadPlayerIds.clear()
            leadPlayerIds.add(it.key)
            leadScore = it.value
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder =
        ScoreViewHolder(LayoutInflater.from(parent.context).inflate(ScoreViewHolder.RESOURCE_ID, parent, false))

    override fun getItemCount(): Int = totalsMap.size

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val playerId = rounds.first().scores[position].player.id
        holder.bindTotal(totalsMap[playerId] ?: 0, leadPlayerIds.contains(playerId))
    }

}

class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val nameHolder: TextView by lazy { itemView.playerNameHeader }

    fun bind(player: Player) {
        nameHolder.text = player.name
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_score_row_header
    }
}

class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val scoreHolder: EditText by lazy { itemView.playerScore }

    fun bind(rounds: List<Round>, round: Round, score: Score, eventConsumer: Consumer<GameEvent>?) {
        if (score.player == round.dealer) {
            scoreHolder.setBackgroundColor(Color.YELLOW)
        } else {
            scoreHolder.setBackgroundColor(Color.WHITE)
        }
        if (!scoreHolder.hasFocus()) {
            // Hack to get score view to stay selected on next focus after an update occurs
            scoreHolder.setText(score.value.toString())
        }
        scoreHolder.isEnabled = true
        scoreHolder.isFocusableInTouchMode = true
        scoreHolder.setOnEditorActionListener { _, _, _ ->
            updateScore(eventConsumer, round, score)
            false
        }
        scoreHolder.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                addRound(eventConsumer, rounds, round)
            } else {
                updateScore(eventConsumer, round, score)
            }
        }
    }

    fun bindTotal(score: Int, isLeader: Boolean) {
        clearBind()
        if (isLeader) {
            scoreHolder.setBackgroundColor(Color.GREEN)
        } else {
            scoreHolder.setBackgroundColor(Color.LTGRAY)
        }
        scoreHolder.setText(score.toString())
    }

    fun clearBind() {
        scoreHolder.setBackgroundColor(Color.WHITE)
        scoreHolder.isFocusable = false
        scoreHolder.isEnabled = false
        scoreHolder.text = null
        scoreHolder.setOnEditorActionListener(null)
        scoreHolder.onFocusChangeListener = null
    }

    // TODO: Remove rounds and move adding a new round to the update loop
    private fun updateScore(eventConsumer: Consumer<GameEvent>?, round: Round, score: Score) {
        // Update the score
        eventConsumer?.accept(UpdateScore(round.id, score.player.id, scoreHolder.text.toString().toInt(), score.metadata))
    }

    private fun addRound(eventConsumer: Consumer<GameEvent>?, rounds: List<Round>, round: Round) {
        // Add a new round if they just added a new score to the last round
        if (rounds.indexOf(round) == rounds.size - 1) {
            eventConsumer?.accept(
                RequestSaveRound(
                    Round(
                        0,
                        round.scores[(round.scores.indexOfFirst { it.player == round.dealer } + 1) % round.scores.size].player,
                        round.number + 1,
                        round.scores.map { Score(player = it.player) }
                    )
                )
            )
        }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_score_row_data
    }
}