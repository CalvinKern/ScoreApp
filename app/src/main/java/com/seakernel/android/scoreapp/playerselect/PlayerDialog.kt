package com.seakernel.android.scoreapp.playerselect

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.core.os.BundleCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.DialogPlayerNameBinding
import com.seakernel.android.scoreapp.playerselect.PlayerDialog.Companion.KEY_DELETE
import com.seakernel.android.scoreapp.playerselect.PlayerDialog.Companion.KEY_PLAYER
import com.seakernel.android.scoreapp.playerselect.PlayerDialog.Companion.REQUEST_PLAYER
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.PlayerColors
import com.seakernel.android.scoreapp.utility.logEvent

/**
 * Adds a player, or changes a player's name and color (or deletes them). What's typed is kept through rotating and
 * Android closing the app, since the dialog comes back with the screen.
 *
 * The choice is sent back with [REQUEST_PLAYER]: the player with their new name and color in [KEY_PLAYER], and
 * [KEY_DELETE] when they should be deleted instead.
 */
class PlayerDialog : DialogFragment() {

    private val player
        get() = BundleCompat.getParcelable(requireArguments(), ARG_PLAYER, Player::class.java)!!

    private val seatedPlayers
        get() = BundleCompat.getParcelableArrayList(requireArguments(), ARG_SEATED_PLAYERS, Player::class.java).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only OK and cancel close it, so a tap outside doesn't throw away what was typed. The fragment controls this,
        // setting it on the dialog builder is overridden.
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val player = player
        val binding = DialogPlayerNameBinding.inflate(layoutInflater)
        val name = { binding.playerNameEdit.text?.toString().orEmpty() }
        lateinit var colorPicker: PlayerColorPicker
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (player.id == null) R.string.playerCreateTitle else R.string.playerEditTitle)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = name()
                if (newName.isEmpty() || !colorPicker.isValid) return@setPositiveButton
                // Player names are personal data, so only the event is logged
                logEvent(if (player.id == null) AnalyticsConstants.Event.PLAYER_CREATE else AnalyticsConstants.Event.PLAYER_RENAMED)
                sendResult(player.copy(name = newName, color = colorPicker.color), delete = false)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .also {
                if (player.id == null) return@also
                it.setNeutralButton(R.string.playerDelete) { _, _ ->
                    logEvent(AnalyticsConstants.Event.PLAYER_DELETED)
                    sendResult(player, delete = true)
                }
            }
            .create()

        // OK needs a name, and a color that can be saved. The fields' text comes back on its own after this is created
        // again, which runs these listeners.
        val updateOk = { dialog.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = name().isNotEmpty() && colorPicker.isValid }
        val seatColor = seatedPlayers.let { PlayerColors.forSeat(requireContext(), it, it.indexOf(player)) }
        colorPicker = PlayerColorPicker(binding, player.color, seatColor, updateOk)
        binding.playerNameEdit.apply {
            setText(player.name)
            setSelection(player.name.length)
            doAfterTextChanged { updateOk() }
        }
        dialog.setOnShowListener { updateOk() }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    private fun sendResult(player: Player, delete: Boolean) {
        setFragmentResult(
            REQUEST_PLAYER,
            Bundle().apply {
                putParcelable(KEY_PLAYER, player)
                putBoolean(KEY_DELETE, delete)
            },
        )
    }

    companion object {
        private const val ARG_PLAYER = "player"
        private const val ARG_SEATED_PLAYERS = "seated_players"

        const val REQUEST_PLAYER = "PLAYER_DIALOG"
        const val KEY_PLAYER = "player"
        const val KEY_DELETE = "delete"

        val TAG: String = PlayerDialog::class.java.name

        /**
         * @param player the player to change, or one without an id to add a new player
         * @param seatedPlayers the game being set up's players in seat order, with [player] in theirs, for its default
         * color
         */
        fun newInstance(player: Player, seatedPlayers: List<Player>) = PlayerDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_PLAYER, player)
                putParcelableArrayList(ARG_SEATED_PLAYERS, ArrayList(seatedPlayers))
            }
        }
    }
}
