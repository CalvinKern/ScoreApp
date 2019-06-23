package com.seakernel.android.scoreapp.utility

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Set the value of the settings on the main thread, as postValue doesn't work actually update if nothing is observing
suspend fun <T> MutableLiveData<T>.safePostValue(value: T) {
    withContext(Dispatchers.Main) {
        this@safePostValue.value = value
    }
}