package com.seakernel.android.scoreapp.game

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
class GameScoreAdapter(private val hasDealer: Boolean, private val rounds: List<Round>, private val eventConsumer: Consumer<GameEvent>? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = (rounds.count() * playerCount())

    override fun getItemId(position: Int): Long = rounds[toRoundIndex(position)].scores[toScoreIndex(position)].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ScoreViewHolder(LayoutInflater.from(parent.context).inflate(ScoreViewHolder.RESOURCE_ID, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val round = rounds[toRoundIndex(position)]
        (holder as ScoreViewHolder).bind(hasDealer, rounds, round, round.scores[toScoreIndex(position)], eventConsumer)
    }

    // Helper functions

    private fun playerCount() = rounds.first().scores.size

    private fun toScoreIndex(position: Int) = position % playerCount()

    private fun toRoundIndex(position: Int): Int = position / playerCount()
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

    fun bind(hasDealer: Boolean, rounds: List<Round>, round: Round, score: Score, eventConsumer: Consumer<GameEvent>?) {
        if (hasDealer && score.player == round.dealer) {
            if (rounds.last().id == round.id) {
                scoreHolder.setBackgroundResource(R.color.dealer)
            } else {
                scoreHolder.setBackgroundResource(R.color.dealerSoft)
            }
        } else {
            // Make odd rows with a slight gray background to look a little better
            if (round.number % 2 == 0) {
                scoreHolder.setBackgroundResource(R.color.slightGray)
            } else {
                scoreHolder.setBackgroundResource(R.color.white)
            }
        }
        if (!scoreHolder.hasFocus()) {
            // Hack to get score view to stay selected on next focus after an update occurs
            scoreHolder.setText(score.value.toString())
        }
        scoreHolder.isEnabled = true
        scoreHolder.isFocusableInTouchMode = true
        scoreHolder.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                addRound(eventConsumer, rounds, round)
            } else {
                updateScore(eventConsumer, round, score)
            }
        }
    }

    fun bindTotal(score: Int, isLeader: Boolean) {
        scoreHolder.isEnabled = false
        scoreHolder.isFocusable = false
        scoreHolder.isFocusableInTouchMode = false

        if (isLeader) {
            scoreHolder.setBackgroundResource(R.color.winnerGreen)
        } else {
            scoreHolder.setBackgroundResource(R.color.black)
        }
        scoreHolder.setText(score.toString())
        scoreHolder.setTextColor(scoreHolder.context.getColor(R.color.white))
    }

    private fun updateScore(eventConsumer: Consumer<GameEvent>?, round: Round, score: Score) {
        val updatedScore = if (scoreHolder.text.isNotBlank()) {
            scoreHolder.text.toString().toInt()
        } else {
            0
        }

        // Update the score
        eventConsumer?.accept(
            GameEvent.UpdateScore(
                round.id,
                score.player.id,
                updatedScore,
                score.metadata
            )
        )
    }

    private fun addRound(eventConsumer: Consumer<GameEvent>?, rounds: List<Round>, round: Round) {
        if (rounds.indexOf(round) == rounds.size - 1) {
            // Add a new round if they just added a new score to the last round
            eventConsumer?.accept(GameEvent.RequestCreateRound)
        }
    }

    companion object {
        const val RESOURCE_ID = R.layout.holder_score_row_data
    }
}