package com.seakernel.android.scoreapp.game.classic

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.calculator.CalculatorKeyboardView
import com.seakernel.android.scoreapp.calculator.CalculatorUtils
import com.seakernel.android.scoreapp.calculator.ScoreKeyListener
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.data.Round
import com.seakernel.android.scoreapp.data.Score
import com.seakernel.android.scoreapp.databinding.HolderRoundAddBinding
import com.seakernel.android.scoreapp.databinding.HolderScoreRowDataBinding
import com.seakernel.android.scoreapp.databinding.HolderScoreRowHeaderBinding
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.seakernel.android.scoreapp.ui.ConfettiHost
import com.seakernel.android.scoreapp.utility.ScoreFormat
import com.seakernel.android.scoreapp.utility.setVisible
import com.seakernel.android.scoreapp.utility.themeColor
import com.spotify.mobius.functions.Consumer
import java.security.InvalidParameterException

private typealias CalculatorKeyboardCallback = (scoreView: EditText?) -> Unit

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class GameScoreAdapter(
    private val hasDealer: Boolean,
    private val useCalculator: Boolean,
    private val rounds: List<Round>,
    private val focusedScoreId: Long?,
    private val isGoalReached: Boolean = false,
    private val eventConsumer: Consumer<GameEvent>? = null,
    private val addRoundButtonOffset: () -> Float = { 0f },
    private val showCalculatorKeyboardCallback: CalculatorKeyboardCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = (rounds.count() * playerCount()) + 1 // add round button

    override fun getItemId(position: Int): Long =
        if (isAddRoundPosition(position)) Long.MAX_VALUE else rounds[toRoundIndex(position)].scores[toScoreIndex(
            position
        )].id

    override fun getItemViewType(position: Int): Int {
        return if (isAddRoundPosition(position)) VIEW_TYPE_ROUND_ADD else VIEW_TYPE_SCORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SCORE -> ScoreViewHolder(parent, showCalculatorKeyboardCallback)
            VIEW_TYPE_ROUND_ADD -> AddRoundViewHolder(parent)
            else -> throw InvalidParameterException("viewType ($viewType) is not supported in the ${GameScoreAdapter::class.java.name}")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_SCORE -> {
                val round = rounds[toRoundIndex(position)]
                (holder as ScoreViewHolder).bind(
                    hasDealer,
                    useCalculator,
                    rounds,
                    round,
                    round.scores[toScoreIndex(position)],
                    focusedScoreId,
                    isGoalReached,
                    eventConsumer,
                )
            }
            VIEW_TYPE_ROUND_ADD -> {
                (holder as AddRoundViewHolder).bind(isGoalReached, eventConsumer, addRoundButtonOffset())
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ScoreViewHolder) {
            holder.onRecycled()
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

        // Rows of score cells the recycled view pool keeps for reuse while scrolling
        private const val RECYCLED_ROWS = 3

        /**
         * The pool keeps 5 views of a type by default, less than one row in larger games, so most of a row that
         * scrolled away was thrown out and inflated again for the next one
         */
        fun sizeRecycledViewPool(pool: RecyclerView.RecycledViewPool, playerCount: Int) {
            pool.setMaxRecycledViews(VIEW_TYPE_SCORE, playerCount * RECYCLED_ROWS)
        }
    }
}

class PlayersAdapter(
    private val showNotes: Boolean,
    private val players: List<Player>,
    private val playerHolderClickedListener: PlayerViewHolder.PlayerHolderClickedListener
) : RecyclerView.Adapter<PlayerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder =
        PlayerViewHolder(
            HolderScoreRowHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun getItemCount(): Int {
        return players.count()
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(showNotes, players[position], playerHolderClickedListener)
    }
}

class TotalsAdapter(
    private val players: List<Player>,
    private val playerTotals: Map<Long, Double>,
    val leadPlayerIds: List<Long>
) :
    RecyclerView.Adapter<ScoreViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // Screen readers hear the whole row at once from its description instead of each total on its own
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder =
        ScoreViewHolder(parent).apply {
            itemView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

    override fun getItemCount(): Int = players.size

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val playerId = players[position].id ?: return
        val score = playerTotals[playerId] ?: 0.0
        holder.bindTotal(score, leadPlayerIds.contains(playerId))
    }

    /**
     * Every player's total in table order, read as one item so it's a single swipe from the add round button,
     * e.g. "Alice, total 12, Leading. Bob, total 9"
     */
    fun describe(context: Context): String =
        players.joinToString(". ") { player ->
            val total = context.getString(
                R.string.playerTotalDescription,
                player.name,
                ScoreFormat.format(playerTotals[player.id] ?: 0.0),
            )
            if (player.id in leadPlayerIds) "$total, ${context.getString(R.string.leading)}" else total
        }

    fun getPlayerPosition(playerId: Long): Int {
        return players.indexOfFirst { it.id == playerId }
    }
}

class PlayerViewHolder(private val binding: HolderScoreRowHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    interface PlayerHolderClickedListener {
        fun playerHolderClicked(player: Player)
    }

    fun bind(
        showNotes: Boolean,
        player: Player,
        clickListener: PlayerHolderClickedListener,
    ) {
        binding.playerNameHeader.text = player.name
        binding.playerNameNoteIcon.setVisible(showNotes)

        // Screen readers hear just the name here, the totals row reads everyone's total and who's leading
        val context = itemView.context
        ViewCompat.replaceAccessibilityAction(
            itemView,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            context.getString(R.string.editNotes),
            null,
        )

        // The Show Round Notes setting only shows the notes icon as a hint. Tapping a name opens the notes either
        // way on purpose, so notes are always reachable, even in games that don't use them much.
        itemView.setOnClickListener { clickListener.playerHolderClicked(player) }
        itemView.setOnLongClickListener { clickListener.playerHolderClicked(player); true }
    }
}

class AddRoundViewHolder(parent: ViewGroup) : BaseViewHolder<HolderRoundAddBinding>(
    HolderRoundAddBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    )
) {
    /**
     * @param horizontalOffset how far to move the button from the middle of the table, see [setHorizontalOffset]
     */
    fun bind(isGoalReached: Boolean, eventConsumer: Consumer<GameEvent>?, horizontalOffset: Float) {
        setHorizontalOffset(horizontalOffset)

        binding.addRoundButton.setVisible(!isGoalReached)
        binding.finishGameButton.setVisible(isGoalReached)

        binding.addRoundButton.setOnClickListener { eventConsumer?.accept(GameEvent.RequestCreateRound) }
        binding.finishGameButton.setOnClickListener { eventConsumer?.accept(GameEvent.RequestFinishGame) }
    }

    /**
     * Moves the button sideways from the middle of the table. When the table is wider than the screen, this keeps
     * it in the middle of the screen as the table scrolls.
     */
    fun setHorizontalOffset(offset: Float) {
        binding.addRoundButton.translationX = offset
        binding.finishGameButton.translationX = offset
    }
}

class ScoreViewHolder(
    parent: ViewGroup,
    private val showCalculatorKeyboardCallback: CalculatorKeyboardCallback? = null
) : BaseViewHolder<HolderScoreRowDataBinding>(
    HolderScoreRowDataBinding.inflate(
        LayoutInflater.from(
            parent.context
        ), parent, false
    )
), TextWatcher {

    init {
        binding.playerScore.keyListener = ScoreKeyListener()
        binding.playerScore.addTextChangedListener(this)
        ViewCompat.setAccessibilityDelegate(binding.playerScore, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                accessibilityHint?.let { info.hintText = it }
                if (hasInvalidInput) {
                    info.isContentInvalid = true
                    info.error = host.context.getString(R.string.incomplete)
                }
            }
        })
    }

    private var _eventConsumer: Consumer<GameEvent>? = null
    private var _round: Round? = null
    private var _score: Score? = null
    private var _focusedScoreId: Long? = null
    private var _isGoalReached: Boolean = false

    /** The text shown and the score saved when the cell gained focus, so unchanged text never rewrites the score */
    private var textOnFocus: String? = null
    private var scoreOnFocus: Score? = null

    /** Text set by the app (instead of typed) shouldn't be saved as a new score */
    private var isSettingText = false

    // Read by screen readers, the score is read as the text, followed by who and which round it's for
    private var accessibilityHint: String? = null
    private var hasInvalidInput = false

    fun bind(
        hasDealer: Boolean,
        useCalculator: Boolean,
        rounds: List<Round>,
        round: Round,
        score: Score,
        focusedScoreId: Long?,
        isGoalReached: Boolean,
        eventConsumer: Consumer<GameEvent>?
    ) {
        // I hate doing this, but it gets the score to update the total immediately
        _eventConsumer = eventConsumer
        _round = round
        _score = score
        _focusedScoreId = focusedScoreId
        _isGoalReached = isGoalReached

        binding.playerScore.showSoftInputOnFocus = !useCalculator

        // Short enough to move through rounds quickly. Only the current dealer is announced, past dealers don't
        // matter during play and would slow down every round.
        val isCurrentDealer = hasDealer && score.player == round.dealer && rounds.last().id == round.id
        val roundNumber = rounds.indexOfFirst { it.id == round.id } + 1
        accessibilityHint = itemView.context.getString(R.string.scoreCellHint, score.player.name, roundNumber)
        ViewCompat.setStateDescription(
            binding.playerScore,
            if (isCurrentDealer) itemView.context.getString(R.string.dealerLabel) else null,
        )

        if (hasDealer && score.player == round.dealer) {
            if (isCurrentDealer) {
                binding.playerBorder.setBackgroundResource(R.drawable.border_dealer)
            } else {
                binding.playerBorder.setBackgroundResource(R.drawable.border_dealer_past)
            }
        } else {
            binding.playerBorder.setBackgroundResource(0)
        }

        // Make odd rows with a slight gray background to look a little better. By position, since saved round numbers
        // keep gaps after rounds are deleted.
        if (roundNumber % 2 == 1) {
            itemView.setBackgroundColor(itemView.context.themeColor(R.attr.colorSurfaceContainer))
        } else {
            itemView.setBackgroundColor(itemView.context.themeColor(R.attr.colorSurface))
        }

        if (!binding.playerScore.hasFocus()) {
            // Leave the text alone while it's being edited
            showSavedScore(score)
        }
        binding.playerScore.isEnabled = true
        binding.playerScore.isFocusable = true
        binding.playerScore.setOnEditorActionListener { _, code, event ->
            when (code) {
                CalculatorKeyboardView.KEYCODE_EQUALS -> {
                    updateScore(commit = true)
                    binding.playerScore.clearFocus()
                }
                CalculatorKeyboardView.KEYCODE_NEXT -> {
                    val isLastScore = score.id == rounds.last().scores.last().id
                    if (event != null) {
                        // A hardware keyboard's enter key sends the action when it's pressed and again when it's
                        // released. Only the release counts: keeping the press sends the release here too, where
                        // moving on at the press would send it to the next score, which moves down a round.
                        if (event.action != KeyEvent.ACTION_UP) return@setOnEditorActionListener true
                        if (!isLastScore) {
                            // On to the next score, like the on-screen key (which the system handles below)
                            binding.playerScore.focusSearch(View.FOCUS_FORWARD)?.requestFocus()
                            return@setOnEditorActionListener true
                        }
                    }

                    // Add a new round only when we're the last round, otherwise let the action propagate to the system
                    if (!isLastScore) return@setOnEditorActionListener false

                    if (isGoalReached) {
                        eventConsumer?.accept(GameEvent.RequestFinishGame)
                    } else {
                        eventConsumer?.accept(GameEvent.RequestCreateRound)
                    }
                }
            }

            true
        }

        binding.playerScore.setOnFocusChangeListener { _, hasFocus ->
            val focusedScore = _score ?: return@setOnFocusChangeListener
            val scoreId = focusedScore.id

            if (!hasFocus) {
                updateScore(commit = true)
                binding.playerScore.error = null // Clear error state when losing focus
                textOnFocus = null
                scoreOnFocus = null
                showSavedScore(focusedScore)

                // Notify Mobius that focus is lost so we don't "sticky focus" on scroll back
                if (scoreId == _focusedScoreId) {
                    showCalculatorKeyboardCallback?.invoke(null)
                    _eventConsumer?.accept(GameEvent.ScoreFocusLost(scoreId))
                }
            } else {
                // Show what was typed (e.g. "10/3") so it can be edited, instead of the rounded score
                val editText = editableText(focusedScore)
                val shownText = binding.playerScore.text.toString()
                textOnFocus = editText
                scoreOnFocus = focusedScore
                setScoreText(editText)
                binding.playerScore.setTextColor(itemView.context.themeColor(R.attr.colorOnSurface))
                // Start at the end of the score (makes quick edits/additions easier). A tap that focused the cell
                // places the cursor where it landed afterwards, which is kept when the text is what was tapped on.
                binding.playerScore.setSelection(editText.length)
                if (editText != shownText) {
                    // The tap landed on the shown score (e.g. "55"), not on the typed input that replaced it
                    // (e.g. "5x4+10+25"), so its spot means nothing. Go back to the end once the tap is handled.
                    binding.playerScore.post {
                        if (binding.playerScore.hasFocus()) binding.playerScore.setSelection(binding.playerScore.length())
                    }
                }

                // Newly gained focus = open calculator/keyboard, which starts from the text set above
                showCalculatorKeyboardCallback?.invoke(binding.playerScore)

                if (scoreId != _focusedScoreId) {
                    // Wait one frame, then double check if the cell still has focus
                    //  If a deleted round was focused, it will be  given to the first cell in the recycler view, when we should wait for the intended cell to be focused
                    binding.playerScore.post {
                        if (binding.playerScore.hasFocus()) {
                            _eventConsumer?.accept(GameEvent.ScoreFocused(scoreId))
                        }
                    }
                }
            }
        }
        if (hasDealer) {
            binding.playerScore.setOnLongClickListener {
                showPlayerDealerDialog(score.player, round, eventConsumer)
                true
            }
        } else {
            // Games without a dealer keep the normal long press behavior of the text field
            binding.playerScore.setOnLongClickListener(null)
        }

        // Handle Focus Persistence and Automatic Focus
        if (score.id == focusedScoreId) {
            if (!binding.playerScore.hasFocus()) {
                binding.playerScore.post {
                    if (_score?.id == _focusedScoreId && !binding.playerScore.hasFocus()) {
                        binding.playerScore.requestFocus()
                    }
                }
            }
        } else if (focusedScoreId != null && binding.playerScore.hasFocus()) {
            // Only give up focus to another score. No focused score means the previous cell's focus loss was
            // handled after this cell was tapped, and its own focus is still on the way. Clearing it here would
            // focus it again without the tap, moving the cursor from where it was tapped to the end.
            binding.playerScore.clearFocus()
        }
    }

    fun celebrate() {
        itemView.post {
            (itemView.context as? ConfettiHost)?.celebrateFrom(binding.playerScore)
        }
    }

    fun onRecycled() {
        if (binding.playerScore.hasFocus()) {
            binding.playerScore.clearFocus()
        }
        _score = null
        _round = null
        _eventConsumer = null
        _focusedScoreId = null
        textOnFocus = null
        scoreOnFocus = null
    }

    fun bindTotal(score: Double, isLeader: Boolean) {
        accessibilityHint = null
        hasInvalidInput = false
        binding.playerScore.isEnabled = false
        binding.playerScore.isFocusable = false

        if (isLeader) {
            binding.playerBorder.setBackgroundResource(R.drawable.border_winner)
        } else {
            binding.playerBorder.setBackgroundResource(0)
        }
        binding.playerScore.setTextColor(itemView.context.themeColor(R.attr.colorOnSurface))
        setScoreText(formatScore(score))
    }

    private fun formatScore(score: Double) = ScoreFormat.format(score)

    private fun setScoreText(text: CharSequence) {
        isSettingText = true
        binding.playerScore.setText(text)
        isSettingText = false
    }

    /**
     * Shows the saved score, or what was typed when it couldn't be calculated (e.g. dividing by zero). Those are
     * saved as 0, and showing the input in the error color makes the mistake easy to spot and fix.
     */
    private fun showSavedScore(score: Score) {
        val input = score.input
        hasInvalidInput = input != null && CalculatorUtils.eval(input) == null
        if (input != null && hasInvalidInput) {
            setScoreText(CalculatorUtils.toDisplayInput(input, itemView.context))
            binding.playerScore.setTextColor(itemView.context.themeColor(R.attr.colorError))
        } else {
            setScoreText(formatScore(score.value))
            binding.playerScore.setTextColor(itemView.context.themeColor(R.attr.colorOnSurface))
        }
    }

    /** The text to edit: what was typed for the score, or the score itself for scores saved before input was kept */
    private fun editableText(score: Score): String = when {
        score.input != null -> CalculatorUtils.toDisplayInput(score.input, itemView.context)
        score.value == 0.0 -> ""
        else -> formatScore(score.value)
    }

    /**
     * Saves the typed text as the score.
     *
     * @param commit whether editing is done. While typing, an incomplete calculation keeps the previous score.
     * Once done, a calculation that can't be completed is saved as 0, keeping the input so it can be fixed.
     */
    private fun updateScore(commit: Boolean) {
        val round = _round ?: return
        val score = _score ?: return
        val text = binding.playerScore.text.toString()
        val originalScore = scoreOnFocus

        val (value, input) = if (originalScore != null && text == textOnFocus) {
            // Back to what was shown, so keep what was saved instead of reading back the rounded text
            originalScore.value to originalScore.input
        } else if (text.isBlank()) {
            0.0 to null
        } else {
            val typedInput = CalculatorUtils.toCanonicalInput(text, itemView.context)
            val result = CalculatorUtils.eval(typedInput)?.toDoubleOrNull()
            when {
                result != null -> result to typedInput
                commit -> 0.0 to typedInput
                else -> return // Still typing
            }
        }

        if (score.value == value && score.input == input) return

        _eventConsumer?.accept(
            GameEvent.UpdateScore(
                round.id!!,
                score.player.id!!,
                value,
                input,
            )
        )
    }

    private fun showPlayerDealerDialog(
        player: Player,
        round: Round,
        eventConsumer: Consumer<GameEvent>?
    ) {
        val dialog = MaterialAlertDialogBuilder(itemView.context)
            .setMessage(itemView.context.getString(R.string.makePlayerDealerMessage, player.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.setDealer) { _, _ ->
                eventConsumer?.accept(GameEvent.RequestSaveRound(round.copy(dealer = player)))
            }
            .create()
        dialog.show()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        // Only text typed into the focused cell is a new score, and it updates the total immediately
        if (isSettingText || !binding.playerScore.hasFocus()) return
        updateScore(commit = false)
    }
}
