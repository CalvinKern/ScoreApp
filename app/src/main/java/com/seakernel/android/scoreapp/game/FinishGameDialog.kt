package com.seakernel.android.scoreapp.game

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.game.classic.GameFragment

/**
 * Asks to finish the game once its goal is reached, or keep playing with another round.
 * The choice is sent back with [GameFragment.REQUEST_FINISH_GAME] or [GameFragment.REQUEST_CONTINUE_GAME].
 */
class FinishGameDialog : DialogFragment() {

    companion object {
        fun newInstance() = FinishGameDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.finishGameTitle)
            .setMessage(R.string.finishGameConfirmation)
            .setPositiveButton(R.string.finish) { _, _ ->
                parentFragmentManager.setFragmentResult(GameFragment.REQUEST_FINISH_GAME, Bundle())
            }
            .setNegativeButton(R.string.continuePlaying) { _, _ ->
                parentFragmentManager.setFragmentResult(GameFragment.REQUEST_CONTINUE_GAME, Bundle())
            }
            .create()
    }
}
