package com.seakernel.android.scoreapp.utility

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Usage statistics (Analytics) and crash reports (Crashlytics) are shared only with the user's consent, asked for
 * both at once with [com.seakernel.android.scoreapp.ui.UsageDataConsentDialog] and changed separately in settings
 * afterwards. Both are off in the manifest, so nothing is collected before the user answers.
 */
object UsageData {

    /** Saves the user's answer for both usage statistics and crash reports and applies it */
    fun setShareEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setShareUsageDataEnabled(context, enabled)
        AppPreferences.setShareCrashReportsEnabled(context, enabled)
        applyUsageStatistics(context)
        applyCrashReports(context, deleteUnsent = !enabled)
    }

    /** Saves the user's choice for usage statistics and applies it */
    fun setUsageStatisticsEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setShareUsageDataEnabled(context, enabled)
        applyUsageStatistics(context)
    }

    /** Saves the user's choice for crash reports and applies it */
    fun setCrashReportsEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setShareCrashReportsEnabled(context, enabled)
        applyCrashReports(context, deleteUnsent = !enabled)
    }

    /** Applies the saved choices. Nothing is shared until the user has answered */
    fun apply(context: Context) {
        applyUsageStatistics(context)
        applyCrashReports(context, deleteUnsent = false)
    }

    private fun applyUsageStatistics(context: Context) {
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
    }

    private fun applyCrashReports(context: Context, deleteUnsent: Boolean) {
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(AppPreferences.isShareCrashReportsEnabled(context))
            // Crash reports kept while waiting for an answer are sent once allowed, and shouldn't be kept otherwise
            if (deleteUnsent) deleteUnsentReports()
        }
    }
}
