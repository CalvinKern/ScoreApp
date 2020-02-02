package com.seakernel.android.scoreapp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import timber.log.Timber

/**
 * Created by Calvin on 12/15/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
class ScoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(Timber.DebugTree()) // Just use the debug tree as the production one for now
        }
    }
}