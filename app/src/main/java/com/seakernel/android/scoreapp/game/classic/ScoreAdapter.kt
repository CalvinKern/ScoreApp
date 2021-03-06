package com.seakernel.android.scoreapp.game.classic

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.calculator.CalculatorKeyboardView
import com.seakernel.android.scoreapp.calculator.CalculatorUtils
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.seakernel.android.scoreapp.utility.setVisible
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.holder_score_row_data.view.*
import kotlinx.android.synthetic.main.holder_score_row_header.view.*
import java.security.InvalidParameterException
import java.text.DecimalFormat

private typealias CalculatorKeyboardCallback = (scoreView: EditText) -> Unit
/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameScoreAdapter(
    private val hasDealer: Boolean,
    private val useCalculator: Boolean,
    private val rounds: List<Round>,
    private val eventConsumer: Consumer<GameEvent>? = null,
    private val showCalculatorKeyboardCallback: CalculatorKeyboardCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            VIEW_TYPE_SCORE -> ScoreViewHolder(parent, showCalculatorKeyboardCallback)
            VIEW_TYPE_ROUND_ADD -> AddRoundViewHolder(parent)
            else -> throw InvalidParameterException("viewType ($viewType) is not supported in the ${GameScoreAdapter::class.java.simpleName}")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)) {
            VIEW_TYPE_SCORE -> {
                val round = rounds[toRoundIndex(position)]
                (holder as ScoreViewHolder).bind(hasDealer, useCalculator, rounds, round, round.scores[toScoreIndex(position)], eventConsumer)
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

class PlayersAdapter(private val showNotes: Boolean, private val players: List<Player>, private val playerHolderClickedListener: PlayerViewHolder.PlayerHolderClickedListener) : RecyclerView.Adapter<PlayerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder =
        PlayerViewHolder(
            LayoutInflater.from(parent.context).inflate(
                PlayerViewHolder.RESOURCE_ID,
                parent,
                false
            )
        )

    override fun getItemCount(): Int {
        return players.count()
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(showNotes, players[position], playerHolderClickedListener)
    }
}

class TotalsAdapter(private val reversedScoring: Boolean, private val rounds: List<Round>) : RecyclerView.Adapter<ScoreViewHolder>() {
    private val leadPlayerIds: ArrayList<Long> = arrayListOf()
    private val totalsMap: HashMap<Long, Double> = HashMap(rounds.size) // PlayerID to total

    init {
        setHasStableIds(true)

        rounds.forEach { round ->
            round.scores.forEach { score ->
                totalsMap[score.player.id!!] = (totalsMap[score.player.id] ?: 0.0) + score.value
            }
        }

        var leadScore: Double? = null
        totalsMap.forEach {
            if (leadScore != null && leadScore!! > it.value) {
                if (reversedScoring) leadPlayerIds.clear() else return@forEach
            }
            if (leadScore != null && leadScore!! < it.value) {
                if (reversedScoring) return@forEach else leadPlayerIds.clear()
            }
            leadPlayerIds.add(it.key)
            leadScore = it.value
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder =
        ScoreViewHolder(parent)

    override fun getItemCount(): Int = totalsMap.size

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val playerId = rounds.first().scores[position].player.id
        holder.bindTotal(totalsMap[playerId] ?: 0.0, leadPlayerIds.contains(playerId))
    }
}

class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    interface PlayerHolderClickedListener {
        fun playerHolderClicked(player: Player)
    }

    private val nameHolder: TextView by lazy { itemView.playerNameHeader }
    private val playerNameNoteIcon: View by lazy { itemView.playerNameNoteIcon }

    fun bind(showNotes: Boolean, player: Player, clickListener: PlayerHolderClickedListener) {
        nameHolder.text = player.name
        playerNameNoteIcon.setVisible(showNotes)

        itemView.setOnClickListener { clickListener.playerHolderClicked(player) }
        itemView.setOnLongClickListener { clickListener.playerHolderClicked(player); true }
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

class ScoreViewHolder(
    parent: ViewGroup,
    private val showCalculatorKeyboardCallback: CalculatorKeyboardCallback? = null
) : BaseViewHolder(parent, R.layout.holder_score_row_data) {

    var shouldFocus: Boolean = true
    private val scoreHolder: EditText by lazy {
        itemView.playerScore
    }

    fun bind(hasDealer: Boolean, useCalculator: Boolean, rounds: List<Round>, round: Round, score: Score, eventConsumer: Consumer<GameEvent>?) {
        scoreHolder.showSoftInputOnFocus = !useCalculator

        if (hasDealer && score.player == round.dealer) {
            if (rounds.last().id == round.id) {
                itemView.setBackgroundResource(R.color.dealer)
            } else {
                itemView.setBackgroundResource(R.color.dealerPast)
            }
        } else {
            // Make odd rows with a slight gray background to look a little better
            if (round.number % 2 == 0) {
                itemView.setBackgroundResource(R.color.slightGray)
            } else {
                itemView.setBackgroundResource(R.color.colorBackground)
            }
        }
        if (!scoreHolder.hasFocus()) {
            // Hack to get score view to stay selected on next focus after an update occurs
            scoreHolder.setText(formatScore(score.value))
        }
        scoreHolder.isEnabled = true
        scoreHolder.isFocusable = true
        scoreHolder.setTextColor(scoreHolder.context.getColor(R.color.textBlack))
        scoreHolder.setOnEditorActionListener { _, code, _ ->
            when (code) {
                CalculatorKeyboardView.KEYCODE_EQUALS -> updateScore(eventConsumer, round, score)
                CalculatorKeyboardView.KEYCODE_NEXT -> {
                    // Add a new round only when we're the last round, otherwise let the action propagate to the system
                    if (score.id != rounds.last().scores.last().id) return@setOnEditorActionListener false

                    eventConsumer?.accept(GameEvent.RequestCreateRound)
                }
            }

            true
        }
        scoreHolder.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateScore(eventConsumer, round, score)
                scoreHolder.error = null // Clear error state when losing focus
            } else {
                // Newly gained focus = open calculator
                if (useCalculator) showCalculatorKeyboardCallback?.invoke(scoreHolder)

                // Set the selection to the end of the score (makes quick edits/additions easier)
                if (score.value == 0.0) {
                    scoreHolder.setText("")
                } else {
                    scoreHolder.setSelection(scoreHolder.text.length)
                }
            }
        }
        scoreHolder.setOnLongClickListener {
            showPlayerDealerDialog(score.player, round, eventConsumer)
            true
        }

        // Moved to the end so we don't muck too much with other focus logic
        // If it's the first score in the last round, request focus (to save the previous rounds score)
        // TODO: Should just debounce changes to save instead of this hack (then it will save on back/settings navigation too)
        if (score.id == rounds.last().scores.first().id && shouldFocus) {
            scoreHolder.requestFocus()
            shouldFocus = false // Reset the focus flag so we don't constantly gain focus
        }
    }

    fun bindTotal(score: Double, isLeader: Boolean) {
        scoreHolder.isEnabled = false
        scoreHolder.isFocusable = false

        if (isLeader) {
            itemView.setBackgroundResource(R.color.winnerBackground)
            scoreHolder.setTextColor(ContextCompat.getColor(itemView.context, R.color.winnerText))
        } else {
            itemView.setBackgroundResource(R.color.black)
            scoreHolder.setTextColor(scoreHolder.context.getColor(R.color.textWhite))
        }
        scoreHolder.setText(formatScore(score))
    }

    private fun formatScore(score: Double) = DecimalFormat("#.##").format(score)

    private fun updateScore(eventConsumer: Consumer<GameEvent>?, round: Round, score: Score) {
        val updatedScore = if (scoreHolder.text.isNotBlank()) {
            CalculatorUtils.eval(scoreHolder.text.toString(), itemView.context)?.toDoubleOrNull()
                ?: score.value
        } else {
            0.0
        }

        // Update the score
        eventConsumer?.accept(
            GameEvent.UpdateScore(
                round.id!!,
                score.player.id!!,
                updatedScore,
                score.metadata
            )
        )
    }

    private fun showPlayerDealerDialog(player: Player, round: Round, eventConsumer: Consumer<GameEvent>?) {
        val dialog = AlertDialog.Builder(itemView.context)
            .setMessage(itemView.context.getString(R.string.makePlayerDealerMessage, player.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.dealerLabel) { _, _ ->
                eventConsumer?.accept(GameEvent.RequestSaveRound(round.copy(dealer = player)))
            }
            .create()
        dialog.show()
    }
}
