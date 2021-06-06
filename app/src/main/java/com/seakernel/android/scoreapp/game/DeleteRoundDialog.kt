package com.seakernel.android.scoreapp.game

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.repository.RoundRepository
import com.seakernel.android.scoreapp.ui.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DeleteRoundDialog(private val roundIds: List<Long>) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rounds = Array(roundIds.size) { i -> getString(R.string.deleteRoundItem, i + 1)}
        val selectedRoundIds = arrayListOf<Long>()

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(getString(R.string.deleteRounds))
            .setView(view)
            .setMultiChoiceItems(rounds, BooleanArray(rounds.size)){ _, which, isChecked ->
                if (isChecked) {
                    selectedRoundIds.add(roundIds[which])
                } else {
                    selectedRoundIds.remove(roundIds[which])
                }
            }
            .setNegativeButton(R.string.actionClose, null)
            .setPositiveButton(R.string.actionSave) { _, _ ->
                GlobalScope.launch {
                    RoundRepository(requireContext()).deleteRounds(*selectedRoundIds.toLongArray())
                }
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
    }
}
