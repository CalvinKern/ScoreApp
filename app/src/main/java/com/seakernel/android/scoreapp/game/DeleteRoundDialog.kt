package com.seakernel.android.scoreapp.game

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.game.classic.GameFragment
import com.seakernel.android.scoreapp.repository.RoundRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private var isDeleting = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rounds = Array(roundIds.size) { i -> getString(R.string.deleteRoundItem, i + 1) }
        val selectedRoundIds = arrayListOf<Long>()

        val alertDialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
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
            .setPositiveButton(R.string.delete, null)
            .create()

        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (isDeleting) return@setOnClickListener

                val idsToDelete = selectedRoundIds.toLongArray()
                if (idsToDelete.isEmpty()) {
                    alertDialog.dismiss()
                    return@setOnClickListener
                }

                isDeleting = true
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                val appContext = requireContext().applicationContext
                val fm = parentFragmentManager
                lifecycleScope.launch(Dispatchers.IO) {
                    RoundRepository(appContext).deleteRounds(*idsToDelete)
                    withContext(Dispatchers.Main) {
                        fm.setFragmentResult(
                            GameFragment.REQUEST_DELETE_ROUND,
                            Bundle()
                        )
                        alertDialog.dismiss()
                    }
                }
            }
        }

        return alertDialog
    }
}
