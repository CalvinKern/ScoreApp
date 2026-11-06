package com.seakernel.android.scoreapp

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.jakewharton.threetenabp.AndroidThreeTen
import com.seakernel.android.scoreapp.utility.AppPreferences
import com.seakernel.android.scoreapp.utility.UsageData
import timber.log.Timber

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class ScoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        // Nothing is shared until the user agrees to it
        UsageData.apply(this)

        // Use wallpaper-based colors on Android 12+ unless the user turned them off; older devices
        // (and users who opt out) keep the app's own color scheme
        DynamicColors.applyToActivitiesIfAvailable(
            this,
            DynamicColorsOptions.Builder()
                .setPrecondition { activity, _ -> AppPreferences.isDynamicColorEnabled(activity) }
                .build()
        )

        // Logs only in debug builds, release builds report errors through Crashlytics instead
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}