package com.seakernel.android.scoreapp.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.utility.AppPreferences
import com.seakernel.android.scoreapp.utility.UsageData

/**
 * Asks once whether to share usage statistics and crash reports, which stay off until the user answers.
 *
 * It can't be dismissed without answering: closing it isn't agreeing, so it would have to stay off and ask again
 * anyway. Both answers are equally easy to pick. One answer covers both to keep it simple, and each can be changed
 * separately later in settings.
 */
class UsageDataConsentDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext().applicationContext
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.usageDataConsentTitle)
            .setMessage(R.string.usageDataConsentMessage)
            .setPositiveButton(R.string.usageDataConsentAllow) { _, _ -> UsageData.setShareEnabled(context, true) }
            .setNegativeButton(R.string.usageDataConsentDeny) { _, _ -> UsageData.setShareEnabled(context, false) }
            .create()
    }

    companion object {
        private val TAG = UsageDataConsentDialog::class.java.name

        /** Asks if the user hasn't answered yet (and isn't being asked already) */
        fun showIfNeeded(activity: FragmentActivity) {
            if (AppPreferences.hasAnsweredShareUsageData(activity)) return
            if (activity.supportFragmentManager.findFragmentByTag(TAG) != null) return
            UsageDataConsentDialog().show(activity.supportFragmentManager, TAG)
        }
    }
}
