package com.seakernel.android.scoreapp.utility

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
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

/**
 * Keeps a press that lifts outside this view from counting as a click. A view clicks on any lift after a press, and
 * only a move outside it cancels the press, so a swipe whose moves never reached the view would click what it started
 * on. adb's `input swipe` drops all of its moves when the app is slow to take the press.
 */
@SuppressLint("ClickableViewAccessibility") // Clicks still go through performClick, only far lifts are cancelled
fun View.ignoreLiftsOutside() {
    val slop = ViewConfiguration.get(context).scaledTouchSlop
    setOnTouchListener { view, event ->
        val outside = event.x < -slop || event.y < -slop || event.x > view.width + slop || event.y > view.height + slop
        if (event.actionMasked != MotionEvent.ACTION_UP || !outside) return@setOnTouchListener false

        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        view.onTouchEvent(cancel)
        cancel.recycle()
        true
    }
}

/** Resolves a color attribute (e.g. `R.attr.colorOnSurface`) from this context's theme */
@ColorInt
fun Context.themeColor(@AttrRes attr: Int): Int =
    MaterialColors.getColor(this, attr, "Theme color attribute not set")

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

/**
 * @param includeStart false for a view along the end edge of the screen, which has no start side to keep clear
 */
fun View.applyWindowInsetsNavigationPadding(includeBottom: Boolean = true, includeStart: Boolean = true) {
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

        val rtl = v.layoutDirection == View.LAYOUT_DIRECTION_RTL
        v.updatePadding(
            left = (if (includeStart || rtl) insets.left else 0) + initialPaddingLeft,
            right = (if (includeStart || !rtl) insets.right else 0) + initialPaddingRight,
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

/** Shows the icons of the menu items in the overflow menu, which the toolbar hides by default */
@SuppressLint("RestrictedApi") // MenuBuilder is restricted, but this is the common way to show overflow icons
fun MaterialToolbar.setOptionalIconsVisible() {
    (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
}

/**
 * Shows a status under the title, or hides it when null. Screen readers hear the change when [announce] is true, but
 * never the first status, which is read along with the rest of the screen.
 */
fun MaterialToolbar.setStatusSubtitle(status: CharSequence?, announce: Boolean) {
    if (subtitle?.toString() == status?.toString()) return

    // The toolbar doesn't share its subtitle view, so it's found by its text. It only announces once it has a status.
    val current = subtitle?.toString()
    children.filterIsInstance<TextView>().firstOrNull { current != null && it.text.toString() == current }?.let {
        it.accessibilityLiveRegion =
            if (announce) View.ACCESSIBILITY_LIVE_REGION_POLITE else View.ACCESSIBILITY_LIVE_REGION_NONE
    }
    subtitle = status
}
