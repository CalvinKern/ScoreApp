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
}
