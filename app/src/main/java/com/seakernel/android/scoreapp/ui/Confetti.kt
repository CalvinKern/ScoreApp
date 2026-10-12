package com.seakernel.android.scoreapp.ui

import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.view.doOnDetach
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.xml.listeners.OnParticleSystemUpdateListener
import java.util.concurrent.TimeUnit

/** The app's celebrations, all in the same colors and shapes */
object Confetti {
    private val colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def)
    private val shapes = listOf(Shape.Square, Shape.Circle)
    private val sizes = listOf(Size.SMALL, Size.MEDIUM)

    /** A medium, circular burst from a point, in pixels within the confetti view */
    fun burst(x: Float, y: Float) = Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        spread = 360, // Full circle so it pops out everywhere
        angle = Angle.TOP, // Biased upward; use with spread
        colors = colors,
        shapes = shapes,
        size = sizes,
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
        position = Position.Absolute(x, y),
    )

    /** A burst from each top corner, shooting down and in toward the middle of the screen */
    fun fromTopCorners() = listOf(
        fromTopCorner(x = 0.0, angle = Angle.RIGHT + CORNER_ANGLE),
        fromTopCorner(x = 1.0, angle = Angle.LEFT - CORNER_ANGLE),
    )

    private fun fromTopCorner(x: Double, angle: Int) = Party(
        speed = 30f,
        maxSpeed = 60f,
        damping = 0.9f,
        spread = 45,
        angle = angle,
        colors = colors,
        shapes = shapes,
        size = sizes,
        timeToLive = 3000L,
        emitter = Emitter(duration = 300, TimeUnit.MILLISECONDS).max(150),
        position = Position.Relative(x, 0.0),
    )

    /**
     * Shows [parties] across the screen, above the window [anchor] is in. A dialog is its own window, drawn over the
     * activity's confetti, so this puts the confetti in a window just above it that ignores touches, and is removed
     * once the confetti is done or the anchor's window closes.
     */
    fun showAbove(anchor: View, parties: List<Party>) {
        val windowManager = anchor.context.getSystemService(WindowManager::class.java)
        val konfetti = KonfettiView(anchor.context)
        val remove = { if (konfetti.isAttachedToWindow) windowManager.removeViewImmediate(konfetti) }
        konfetti.onParticleSystemUpdateListener = object : OnParticleSystemUpdateListener {
            override fun onParticleSystemStarted(view: KonfettiView, party: Party, activeSystems: Int) = Unit
            override fun onParticleSystemEnded(view: KonfettiView, party: Party, activeSystems: Int) {
                if (activeSystems == 0) view.post(remove)
            }
        }

        // The whole screen, bars included, rather than just the anchor's window
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            LayoutParams.TYPE_APPLICATION_PANEL,
            LayoutParams.FLAG_NOT_TOUCHABLE or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            token = anchor.windowToken
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) fitInsetsTypes = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        windowManager.addView(konfetti, params)
        anchor.doOnDetach { remove() }
        konfetti.start(parties)
    }

    private const val CORNER_ANGLE = 45 // Degrees down from straight across
}
