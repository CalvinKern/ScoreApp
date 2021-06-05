package com.seakernel.android.scoreapp.utility

import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Created by Calvin on 2/5/21.
 * Copyright © 2021 SeaKernel. All rights reserved.
 */
object AnalyticsConstants {
    object Event {
        const val FAILED_RATING_DIALOG = "FAILED_RATING"
        const val DEALER_REPLACED = "DEALER_REPLACED"
        const val GAME_CREATED = "GAME_CREATED"
        const val GAME_DELETED = "GAME_DELETED"
        const val GAME_LOADED = "GAME_LOADED"
        const val GAME_PLAYER_ADDED = "GAME_PLAYER_ADDED"
        const val PLAYER_CREATE = "PLAYER_CREATED"
        const val PLAYER_DELETED = "PLAYER_DELETED"
        const val PLAYER_RENAMED = "PLAYER_RENAMED"
        const val SHOW_CHANGELOG = "SHOW_CHANGELOG"
        const val SHOW_RATING_DIALOG = "SHOW_RATING"
        const val SHOW_PLAYER_STANDING_DIALOG = "SHOW_PLAYER_STANDING"
        const val SHOW_ROUND_NOTES_DIALOG = "SHOW_ROUND_NOTES"
        const val SHOW_ROUND_DELETE_DIALOG = "DELETE_ROUND_DIALOG"
        const val TOGGLE_GAME_SETTING = "GAME_SETTING_CHANGED"
    }


    object Param {
        const val GAME_PLAYER_COUNT = FirebaseAnalytics.Param.NUMBER_OF_PASSENGERS
        const val ITEM_NAME = FirebaseAnalytics.Param.ITEM_NAME
        const val MESSAGE = FirebaseAnalytics.Param.CONTENT
    }

    sealed class ScreenName(val value: String) {
        object MainActivity : ScreenName("MainActivity")
        object GraphFragment : ScreenName("GraphFragment")
        object GameFragment : ScreenName("GameFragment")
        object GameListFragment : ScreenName("GameListFragment")
        object GameSetupFragment : ScreenName("GameSetupFragment")
        object PlayerSelectFragment : ScreenName("PlayerSelectFragment")
    }
}