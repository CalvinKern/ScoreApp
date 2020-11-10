package com.seakernel.android.scoreapp.utility

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.seakernel.android.scoreapp.ui.MainActivity

fun AppCompatCheckBox.isCheckedSafe(checked: Boolean, listener: CompoundButton.OnCheckedChangeListener?) {
    this.setOnCheckedChangeListener(null)
    isChecked = checked
    this.setOnCheckedChangeListener(listener)
}

fun View.setVisible(visible: Boolean = true) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.setBackgroundRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

// Converts px to dp
val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

// converts dp to px
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

// Analytics helpers
// Key:
//

private typealias BundleBlock = Bundle.() -> Unit
private fun logEvent(context: Context, eventName: String, bundleBlock: BundleBlock) {
    FirebaseAnalytics.getInstance(context).logEvent(eventName, Bundle().apply(bundleBlock))
}

fun Fragment.logScreenView(screenName: String) {
    logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        putString(FirebaseAnalytics.Param.SCREEN_CLASS, MainActivity::class.java.name)
    }
}

fun Fragment.logEvent(eventName: String, bundleBlock: BundleBlock = {}) {
    logEvent(requireContext(), eventName, bundleBlock)
}

fun RecyclerView.ViewHolder.logEvent(eventName: String, bundleBlock: BundleBlock = {}) {
    itemView.logEvent(eventName, bundleBlock)
}

fun View.logEvent(eventName: String, bundleBlock: BundleBlock = {}) {
    logEvent(context, eventName, bundleBlock)
}
