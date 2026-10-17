package com.seakernel.android.scoreapp.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Mobius
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.Update
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fragments that inherit from this will have to provide initialization for a MobiusLoop.Builder and a
 * MobiusLoop.Controller, based on the necessary Model, Event, and Effect classes that will be used.
 *
 * All lifecycle management of the controller is contained in this class, only handling of mobius loop events are
 * required for subclasses. This includes initialization of the loop, connecting views to the event consumer, and
 * connecting effects to the event consumer.
 *
 * Note: Effect handling is a tricky beast (to me), so may be moved to an outside source. Regardless, it's still
 * connected to mobius through the subclasses initialization details, so outside effect handlers may be used.
 *
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
abstract class MobiusFragment<M : Any, E : Any, F : Any> : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.connect(::connectViews)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.disconnect()
    }

    override fun onStart() {
        super.onStart()
        controller.start()
    }

    override fun onStop() {
        super.onStop()
        controller.stop()
    }

    // Mobius - declarations here to make subclasses easier to implement (so that boiler plate gets generated correctly)

    lateinit var loop: MobiusLoop.Builder<M, E, F>
    lateinit var controller: MobiusLoop.Controller<M, E>

    abstract fun initMobius(model: M): First<M, F>
    abstract fun connectViews(eventConsumer: Consumer<E>): Connection<M>
    abstract fun effectHandler(eventConsumer: Consumer<E>): Connection<F>

    /**
     * Effect handlers run on a background thread, so UI work (like navigation or dialogs) has to move to the
     * main thread. It waits until the fragment is started, so fragment transactions can't happen after the
     * state is saved, and is dropped if the fragment is destroyed first.
     */
    protected fun runWhenStarted(block: () -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.withStarted { block() }
        }
    }

    /** Builds the loop so that errors in [update] or [effectHandler] are reported instead of silently dropped */
    protected fun createLoop(update: Update<M, E, F>): MobiusLoop.Builder<M, E, F> =
        Mobius.loop(update) { eventConsumer: Consumer<E> ->
            effectHandler(eventConsumer).reportingErrors { context }
        }.logger(ReportingLogger { context })
}