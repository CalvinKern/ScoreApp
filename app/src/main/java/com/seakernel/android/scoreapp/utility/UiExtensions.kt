package com.seakernel.android.scoreapp.utility

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics

fun AppCompatCheckBox.isCheckedSafe(
    checked: Boolean,
    listener: CompoundButton.OnCheckedChangeListener?,
) {
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

private typealias BundleBlock = Bundle.() -> Unit

private fun logEvent(context: Context, eventName: String, bundleBlock: BundleBlock) {
    FirebaseAnalytics.getInstance(context).logEvent(eventName, Bundle().apply(bundleBlock))
}

fun Fragment.logScreenView(screenName: AnalyticsConstants.ScreenName) {
    logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName.value)
        putString(
            FirebaseAnalytics.Param.SCREEN_CLASS,
            AnalyticsConstants.ScreenName.MainActivity.value
        )
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

fun View.applyWindowInsetsCutout() {
    // Cache the padding to reuse in the listener
    val initialPaddingLeft = paddingLeft
    val initialPaddingRight = paddingRight
    val initialPaddingTop = paddingTop

    val initialHeight = layoutParams.height

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.statusBars()
                    or WindowInsetsCompat.Type.navigationBars()
        )
        v.updatePadding(
            left = insets.left + initialPaddingLeft,
            top = insets.top + initialPaddingTop,
            right = insets.right + initialPaddingRight,
        )
        if (v.layoutParams.height > 0) {
            v.updateLayoutParams {
                height = initialHeight + insets.top
            }
        }

        windowInsets.inset(insets.left, insets.top, insets.right, 0)
    }
}

fun View.applyWindowInsetsNavigationPadding(includeBottom: Boolean = true) {
    // Cache the padding to reuse in the listener
    val initialPaddingLeft = paddingLeft
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.mandatorySystemGestures()
                    or WindowInsetsCompat.Type.ime()
                    or WindowInsetsCompat.Type.displayCutout()
        )

        v.updatePadding(
            left = insets.left + initialPaddingLeft,
            right = insets.right + initialPaddingRight,
        )
        if (includeBottom) {
            v.updatePadding(bottom = insets.bottom + initialPaddingBottom)
            WindowInsetsCompat.CONSUMED
        } else {
            windowInsets.inset(insets.left, insets.top, insets.right, 0)
        }
    }
}

fun View.applyWindowInsetsNavigationMargin() {
    // Cache the margin to reuse in the listener
    val initialMarginLeft = marginLeft
    val initialMarginRight = marginRight
    val initialMarginBottom = marginBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.ime()
        )
        // Apply the insets as a margin to the view
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left + initialMarginLeft
            bottomMargin = insets.bottom + initialMarginBottom
            rightMargin = insets.right + initialMarginRight
        }

        windowInsets.inset(insets.left, 0, insets.right, insets.bottom)
    }
}
