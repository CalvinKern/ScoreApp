package com.seakernel.android.scoreapp.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.seakernel.android.scoreapp.R
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.Next
import timber.log.Timber

/**
 * Mobius runs the update function and effect handlers on executors that drop anything they throw, so a
 * failure (like a database constraint) would make an action silently do nothing. Errors are reported to
 * Crashlytics instead, and the user is told their action didn't go through.
 */
object MobiusErrors {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun report(context: Context?, error: Throwable) {
        Timber.e(error, "Error in a Mobius loop")
        FirebaseCrashlytics.getInstance().recordException(error)

        val appContext = context?.applicationContext ?: return
        mainHandler.post { Toast.makeText(appContext, R.string.errorGeneric, Toast.LENGTH_SHORT).show() }
    }
}

/** Reports exceptions thrown by a loop's init or update function */
class ReportingLogger<M, E, F>(private val context: () -> Context?) : MobiusLoop.Logger<M, E, F> {
    override fun beforeInit(model: M) = Unit
    override fun afterInit(model: M, result: First<M, F>) = Unit
    override fun beforeUpdate(model: M, event: E) = Unit
    override fun afterUpdate(model: M, event: E, result: Next<M, F>) = Unit

    override fun exceptionDuringInit(model: M, exception: Throwable) =
        MobiusErrors.report(context(), exception)

    override fun exceptionDuringUpdate(model: M, event: E, exception: Throwable) =
        MobiusErrors.report(context(), exception)
}

/** Reports exceptions thrown while handling an effect */
fun <F> Connection<F>.reportingErrors(context: () -> Context?): Connection<F> {
    val connection = this
    return object : Connection<F> {
        override fun accept(value: F) {
            try {
                connection.accept(value)
            } catch (e: Exception) {
                MobiusErrors.report(context(), e)
            }
        }

        override fun dispose() = connection.dispose()
    }
}
