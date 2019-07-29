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
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.holder_score_row_data.view.*
import kotlinx.android.synthetic.main.holder_score_row_header.view.*
import java.security.InvalidParameterException

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameScoreAdapter(private val hasDealer: Boolean, private val rounds: List<Round>, private val eventConsumer: Consumer<GameEvent>? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = (rounds.count() * playerCount()) + 1 // add round button

    override fun getItemId(position: Int): Long =
        if (isAddRoundPosition(position)) Long.MAX_VALUE else rounds[toRoundIndex(position)].scores[toScoreIndex(position)].id

    override fun getItemViewType(position: Int): Int {
        return if (isAddRoundPosition(position)) VIEW_TYPE_ROUND_ADD else VIEW_TYPE_SCORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SCORE -> ScoreViewHolder(parent)
            VIEW_TYPE_ROUND_ADD -> AddRoundViewHolder(parent)
            else -> throw InvalidParameterException("viewType ($viewType) is not supported in the ${GameScoreAdapter::class.java.simpleName}")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)) {
            VIEW_TYPE_SCORE -> {
                val round = rounds[toRoundIndex(position)]
                (holder as ScoreViewHolder).bind(hasDealer, rounds, round, round.scores[toScoreIndex(position)], eventConsumer)
            }
            VIEW_TYPE_ROUND_ADD -> {
                (holder as AddRoundViewHolder).bind(eventConsumer)
            }
        }
    }

    // Helper functions

    private fun isAddRoundPosition(position: Int) = position == itemCount - 1

    private fun playerCount() = rounds.firstOrNull()?.scores?.size ?: 0

    private fun toScoreIndex(position: Int) = position % playerCount()

    private fun toRoundIndex(position: Int): Int = position / playerCount()

    companion object {
        private const val VIEW_TYPE_SCORE = 0
        private const val VIEW_TYPE_ROUND_ADD = 1
    }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder = ScoreViewHolder(parent)

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

class AddRoundViewHolder(parent: ViewGroup) : BaseViewHolder(parent, R.layout.holder_round_add) {

    fun bind(eventConsumer: Consumer<GameEvent>?) {
        itemView.setOnClickListener {
            eventConsumer?.accept(GameEvent.RequestCreateRound)
        }
    }
}

class ScoreViewHolder(parent: ViewGroup) : BaseViewHolder(parent, R.layout.holder_score_row_data) {

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
        scoreHolder.isFocusable = true
        scoreHolder.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateScore(eventConsumer, round, score)
            }
        }
    }

    fun bindTotal(score: Int, isLeader: Boolean) {
        scoreHolder.isEnabled = false
        scoreHolder.isFocusable = false

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
}
