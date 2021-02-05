package com.seakernel.android.scoreapp.utility

import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Created by Calvin on 2/5/21.
 * Copyright © 2021 SeaKernel. All rights reserved.
 */
object AnalyticsConstants {
    object Event {
        const val ADD_PLAYER_TO_GAME = FirebaseAnalytics.Event.JOIN_GROUP
        const val CREATE_GAME = FirebaseAnalytics.Event.LEVEL_START
        const val CREATE_PLAYER = FirebaseAnalytics.Event.GENERATE_LEAD
        const val DELETE_GAME = FirebaseAnalytics.Event.LEVEL_END
        const val FAILED_RATING_DIALOG = FirebaseAnalytics.Event.REFUND
        const val LOAD_GAME = FirebaseAnalytics.Event.VIEW_ITEM
        const val NEW_DEALER_SELECTED = FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY
        const val SHOW_CHANGELOG = FirebaseAnalytics.Event.BEGIN_CHECKOUT
        const val SHOW_RATING_DIALOG = FirebaseAnalytics.Event.POST_SCORE
        const val SHOW_PLAYER_STANDING_DIALOG = FirebaseAnalytics.Event.VIEW_PROMOTION
        const val SHOW_ROUND_NOTES_DIALOG = FirebaseAnalytics.Event.CAMPAIGN_DETAILS
    }


    object Param {
        const val GAME_NAME = FirebaseAnalytics.Param.ITEM_NAME
        const val GAME_PLAYER_COUNT = FirebaseAnalytics.Param.NUMBER_OF_PASSENGERS
        const val MESSAGE = FirebaseAnalytics.Param.CONTENT
    }
}