package com.seakernel.android.scoreapp.utility

import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatCheckBox

fun AppCompatCheckBox.isCheckedSafe(checked: Boolean, listener: CompoundButton.OnCheckedChangeListener?) {
    this.setOnCheckedChangeListener(null)
    isChecked = checked
    this.setOnCheckedChangeListener(listener)
}
