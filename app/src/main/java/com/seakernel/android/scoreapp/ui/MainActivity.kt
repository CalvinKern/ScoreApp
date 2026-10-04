package com.seakernel.android.scoreapp.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.ActivityMainBinding
import com.seakernel.android.scoreapp.game.classic.GameFragment
import com.seakernel.android.scoreapp.game.graph.GraphFragment
import com.seakernel.android.scoreapp.gamelist.GameListFragment
import com.seakernel.android.scoreapp.gamesetup.GameSetupFragment
import com.seakernel.android.scoreapp.playerselect.PlayerSelectFragment
import com.seakernel.android.scoreapp.settings.SettingsFragment
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

interface ConfettiHost {
    fun celebrateFrom(anchor: View)
}

class MainActivity : AppCompatActivity(), GameListFragment.GameListListener,
    PlayerSelectFragment.PlayerSelectListener, ConfettiHost,
    GameSetupFragment.GameSetupListener, GameFragment.GameListener {

    private lateinit var binding: ActivityMainBinding

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightMode(newConfig)
            isAppearanceLightNavigationBars = isLightMode(newConfig)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.enableEdgeToEdge(window)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightMode(resources.configuration)
            isAppearanceLightNavigationBars = isLightMode(resources.configuration)
        }
        setContentView(binding.root)

        // Add the list fragment if we don't have any state
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragmentContainer,
                    GameListFragment.newInstance(),
                    GameListFragment::class.java.name
                )
                .commit()
        }
    }

    override fun onPlayersSelected(playerIds: List<Long>) {
        popBackStackIfFound(PlayerSelectFragment::class)
        val fragment =
            supportFragmentManager.findFragmentByTag(GameSetupFragment::class.java.name) as GameSetupFragment
        fragment.updateForNewPlayers(playerIds)
    }

    override fun onShowPlayerSelectScreen(playerIds: List<Long>) {
        showFragment(
            PlayerSelectFragment.newInstance(playerIds),
            PlayerSelectFragment::class.java.name
        )
    }

    override fun onShowGameScreen(gameId: Long) {
        popBackStackIfFound(GameFragment::class) // If copying, remove previous game fragment
        popBackStackIfFound(GameSetupFragment::class)
        showFragment(GameFragment.newInstance(gameId), GameFragment::class.java.name)
    }

    override fun onShowCreateGameScreen() {
        showFragment(GameSetupFragment.newInstance(), GameSetupFragment::class.java.name)
    }

    override fun onShowSettingsScreen() {
        showFragment(SettingsFragment.newInstance(), SettingsFragment::class.java.name)
    }

    override fun onGameSettingsSelected(gameId: Long) {
        showFragment(
            GameSetupFragment.newInstance(gameId),
            GameSetupFragment::class.java.name
        )
    }

    override fun onGameUpdated() {
        popBackStackIfFound(GameSetupFragment::class)
    }

    override fun onGraphSelected(gameId: Long) {
        showFragment(GraphFragment.newInstance(gameId), GraphFragment::class.java.name)
    }

    override fun onNewGame(gameId: Long, initialDealerId: Long?) {
        showFragment(
            GameSetupFragment.newInstanceCopy(gameId, initialDealerId),
            GameSetupFragment::class.java.name
        )
    }

    // Helper Functions
    private fun isLightMode(config: Configuration): Boolean =
        (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES

    private fun popBackStackIfFound(clazz: KClass<*>) {
        supportFragmentManager.findFragmentByTag(clazz.java.name)?.let {
            supportFragmentManager.popBackStack() // Get rid of create fragment if it exists
        }
    }

    // TODO: make first param generic (inheriting from fragment) so that it can be used to generate the tag without resolving the super classes name
    private fun showFragment(fragment: Fragment, tag: String) {
        Timber.i("Showing fragment: $tag")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    /**
     * Show a medium, circular burst of confetti from the given anchor
     */
    override fun celebrateFrom(anchor: View) {
        // Anchor center in screen coordinates
        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val anchorCenterX = anchorLoc[0] + anchor.width / 2f
        val anchorCenterY = anchorLoc[1] + anchor.height / 2f

        // Konfetti origin in screen coordinates
        val konfettiLoc = IntArray(2)
        binding.konfettiView.getLocationOnScreen(konfettiLoc)

        // Translate into Konfetti coordinate space
        val x = anchorCenterX - konfettiLoc[0]
        val y = anchorCenterY - konfettiLoc[1]

        binding.konfettiView.start(burstParty(x, y))
    }

    private fun burstParty(x: Float, y: Float) = Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        spread = 360, // Full circle so it pops out everywhere
        angle = Angle.TOP, // Biased upward; use with spread
        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
        shapes = listOf(Shape.Square, Shape.Circle),
        size = listOf(Size.SMALL, Size.MEDIUM),
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
        position = Position.Absolute(x, y) // Takes Pixels in Konfetti view's coordinate space
    )
}
