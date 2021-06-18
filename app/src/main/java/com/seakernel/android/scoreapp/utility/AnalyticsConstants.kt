package com.seakernel.android.scoreapp.utility

import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Created by Calvin on 2/5/21.
 * Copyright © 2021 SeaKernel. All rights reserved.
 */
object AnalyticsConstants {
    object Event {
        const val FAILED_RATING_DIALOG = "failed_rating"
        const val DEALER_REPLACED = "dealer_replaced"
        const val GAME_CREATED = "game_created"
        const val GAME_DELETED = "game_deleted"
        const val GAME_LOADED = "game_loaded"
        const val GAME_PLAYER_ADDED = "game_player_added"
        const val PLAYER_CREATE = "player_created"
        const val PLAYER_DELETED = "player_deleted"
        const val PLAYER_RENAMED = "player_renamed"
        const val SHOW_CHANGELOG = "show_changelog"
        const val SHOW_RATING_DIALOG = "show_rating"
        const val SHOW_PLAYER_STANDING_DIALOG = "show_player_standing"
        const val SHOW_ROUND_NOTES_DIALOG = "show_round_notes"
        const val SHOW_ROUND_DELETE_DIALOG = "delete_round_dialog"
        const val TOGGLE_GAME_SETTING = "game_setting_changed"
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