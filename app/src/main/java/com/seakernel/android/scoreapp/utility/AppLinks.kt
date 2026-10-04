package com.seakernel.android.scoreapp.utility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.play.core.review.ReviewManagerFactory
import com.seakernel.android.scoreapp.R

/**
 * App-level links (rating, changelog) for the settings screen
 */

fun Fragment.rateApp() {
    logEvent(AnalyticsConstants.Event.SHOW_RATING_DIALOG)
    val manager = ReviewManagerFactory.create(requireContext())
    val reviewRequest = manager.requestReviewFlow()
    reviewRequest.addOnCompleteListener { request ->
        if (request.isSuccessful) {
            // We got the ReviewInfo object
            val reviewInfo = request.result
            val flow = manager.launchReviewFlow(activity ?: return@addOnCompleteListener, reviewInfo)
            flow.addOnCompleteListener { _ ->
                // The flow has finished. The API does not indicate whether the user
                // reviewed or not, or even whether the review dialog was shown. Thus, no
                // matter the result, we continue our app flow.
            }
        } else {
            // There was some problem, continue regardless of the result.
            openPlayStore()
            logEvent(AnalyticsConstants.Event.FAILED_RATING_DIALOG) {
                putString(AnalyticsConstants.Param.MESSAGE, request.exception?.message)
            }
        }
    }
}

fun Fragment.openChangelog() {
    logEvent(AnalyticsConstants.Event.SHOW_CHANGELOG)
    openUrl("https://github.com/CalvinKern/ScoreApp/releases", R.string.incomplete)
}

private fun Fragment.openPlayStore() {
    val url = "https://play.google.com/store/apps/details?id=com.seakernel.scorepad"
    openUrl(url, R.string.storeToReview, "com.android.vending")
}

private fun Fragment.openUrl(url: String, errorStringResource: Int, intentPackage: String? = null) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = url.toUri()
        intentPackage?.let { setPackage(it) }
    }

    try {
        startActivity(intent)
    } catch (_: Exception) {
        val clipboard: ClipboardManager? =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

        if (clipboard == null) {
            Toast.makeText(requireContext(), errorStringResource, Toast.LENGTH_SHORT).show()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", url))

            // Only show a toast for Android 12 and lower
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
                Toast.makeText(requireContext(), R.string.copiedUrl, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
