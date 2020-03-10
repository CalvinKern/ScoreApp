package com.seakernel.android.scoreapp.utility

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatCheckBox

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