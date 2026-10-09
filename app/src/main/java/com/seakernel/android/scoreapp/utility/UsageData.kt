package com.seakernel.android.scoreapp.utility

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Usage statistics (Analytics) and crash reports (Crashlytics) are shared only with the user's consent, asked for
 * once with [com.seakernel.android.scoreapp.ui.UsageDataConsentDialog] and changed in settings afterwards.
 * Both are off in the manifest, so nothing is collected before the user answers.
 */
object UsageData {

    /** Saves the user's choice and applies it */
    fun setShareEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setShareUsageDataEnabled(context, enabled)
        apply(context)

        // Crash reports kept while waiting for an answer are sent once allowed, and shouldn't be kept otherwise
        if (!enabled) FirebaseCrashlytics.getInstance().deleteUnsentReports()
    }

    /** Applies the saved choice, which is no sharing until the user has answered */
    fun apply(context: Context) {
        val enabled = AppPreferences.isShareUsageDataEnabled(context)
        val status = if (enabled) ConsentStatus.GRANTED else ConsentStatus.DENIED

        FirebaseAnalytics.getInstance(context).apply {
            setConsent(
                mapOf(
                    ConsentType.ANALYTICS_STORAGE to status,
                    // No ads, so ad data is never allowed
                    ConsentType.AD_STORAGE to ConsentStatus.DENIED,
                    ConsentType.AD_USER_DATA to ConsentStatus.DENIED,
                    ConsentType.AD_PERSONALIZATION to ConsentStatus.DENIED,
                )
            )
            setAnalyticsCollectionEnabled(enabled)
        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }
}
