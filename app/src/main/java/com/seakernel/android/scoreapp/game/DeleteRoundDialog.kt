package com.seakernel.android.scoreapp.game

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.game.classic.GameFragment
import com.seakernel.android.scoreapp.repository.RoundRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteRoundDialog : DialogFragment() {

    companion object {
        private const val KEY_ROUND_IDS = "ROUND_IDS"

        fun newInstance(roundIds: List<Long>): DeleteRoundDialog {
            val args = Bundle().apply {
                putLongArray(KEY_ROUND_IDS, roundIds.toLongArray())
            }
            val fragment = DeleteRoundDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private val roundIds
        get() = requireArguments().getLongArray(KEY_ROUND_IDS)?.toList() ?: emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rounds = Array(roundIds.size) { i -> getString(R.string.deleteRoundItem, i + 1) }
        val selectedRoundIds = arrayListOf<Long>()

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(getString(R.string.deleteRounds))
            .setView(view)
            .setMultiChoiceItems(rounds, BooleanArray(rounds.size)) { _, which, isChecked ->
                if (isChecked) {
                    selectedRoundIds.add(roundIds[which])
                } else {
                    selectedRoundIds.remove(roundIds[which])
                }
            }
            .setNegativeButton(R.string.actionClose, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    RoundRepository(requireContext()).deleteRounds(*selectedRoundIds.toLongArray())
                }
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult(
            GameFragment.REQUEST_DELETE_ROUND,
            Bundle().apply {}
        )
    }
}
