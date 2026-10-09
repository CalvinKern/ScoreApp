package com.seakernel.android.scoreapp.utility

import android.content.Context
import androidx.core.content.edit

/**
 * App-wide user preferences (not tied to a specific game)
 */
object AppPreferences {
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
    private const val KEY_TRUE_BLACK = "true_black_enabled"
    private const val KEY_AUTO_LOAD_GAME_SETTINGS = "auto_load_game_settings_enabled"
    private const val KEY_SHARE_USAGE_DATA = "share_usage_data_enabled"
    private const val KEY_SHARE_CRASH_REPORTS = "share_crash_reports_enabled"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether to use wallpaper-based dynamic colors (only applies on devices that support it) */
    fun isDynamicColorEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }

    /** Whether dark mode uses a pure black background instead of the tinted dark surface */
    fun isTrueBlackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRUE_BLACK, true)

    fun setTrueBlackEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_TRUE_BLACK, enabled) }

    /** Whether picking a previous game name for a new game loads that game's most recent settings and players */
    fun isAutoLoadGameSettingsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_LOAD_GAME_SETTINGS, true)

    fun setAutoLoadGameSettingsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_AUTO_LOAD_GAME_SETTINGS, enabled) }

    /**
     * Whether usage statistics are sent to Firebase Analytics, which is never until the user agrees.
     * Use [UsageData] to change it, so the choice is applied too.
     */
    fun isShareUsageDataEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHARE_USAGE_DATA, false)

    /** Whether the user has chosen to share usage data or not, either when asked or in settings */
    fun hasAnsweredShareUsageData(context: Context): Boolean =
        prefs(context).contains(KEY_SHARE_USAGE_DATA) || prefs(context).contains(KEY_SHARE_CRASH_REPORTS)

    fun setShareUsageDataEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_SHARE_USAGE_DATA, enabled) }

    /**
     * Whether crash reports are sent to Firebase Crashlytics, which is never until the user agrees.
     * Choices saved before the two were separate covered both, so they still apply here.
     * Use [UsageData] to change it, so the choice is applied too.
     */
    fun isShareCrashReportsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHARE_CRASH_REPORTS, isShareUsageDataEnabled(context))

    fun setShareCrashReportsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_SHARE_CRASH_REPORTS, enabled) }
}
