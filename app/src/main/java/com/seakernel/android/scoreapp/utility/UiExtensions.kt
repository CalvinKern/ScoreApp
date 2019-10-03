package com.seakernel.android.scoreapp.utility

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
