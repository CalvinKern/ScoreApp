package com.seakernel.android.scoreapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.game.GameFragment
import com.seakernel.android.scoreapp.game.GraphFragment
import com.seakernel.android.scoreapp.gamesetup.GameSetupFragment
import com.seakernel.android.scoreapp.playerselect.PlayerSelectFragment
import com.seakernel.android.scoreapp.gamelist.GameListFragment
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), GameListFragment.GameListListener, PlayerSelectFragment.PlayerSelectListener,
    GameSetupFragment.GameSetupListener, GameFragment.GameListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add the list fragment if we don't have any state
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, GameListFragment.newInstance(), GameListFragment::class.java.simpleName)
                .commit()
        }
    }

    override fun onPlayersSelected(playerIds: List<Long>) {
        popBackStackIfFound(PlayerSelectFragment::class)
        val fragment =
            supportFragmentManager.findFragmentByTag(GameSetupFragment::class.java.simpleName) as GameSetupFragment
        fragment.updateForNewPlayers(playerIds)
    }

    override fun onShowPlayerSelectScreen(playerIds: List<Long>) {
        showFragment(PlayerSelectFragment.newInstance(playerIds), PlayerSelectFragment::class.java.simpleName)
    }

    override fun onShowGameScreen(gameId: Long) {
        popBackStackIfFound(GameSetupFragment::class)
        showFragment(GameFragment.newInstance(gameId), GameFragment::class.java.simpleName)
    }

    override fun onShowCreateGameScreen() {
        showFragment(GameSetupFragment.newInstance(), GameSetupFragment::class.java.simpleName)
    }

    override fun onGameSettingsSelected(gameId: Long) {
        showFragment(GameSetupFragment.newInstance(gameId), GameSetupFragment::class.java.simpleName)
    }

    override fun onGameUpdated() {
        popBackStackIfFound(GameSetupFragment::class)
    }

    override fun onGraphSelected(gameId: Long) {
        showFragment(GraphFragment.newInstance(gameId), GraphFragment::class.java.simpleName)
    }

    // Helper Functions
    private fun popBackStackIfFound(clazz: KClass<*>) {
        supportFragmentManager.findFragmentByTag(clazz.java.simpleName)?.let {
            supportFragmentManager.popBackStack() // Get rid of create fragment if it exists
        }
    }

    // TODO: make first param generic (inheriting from fragment) so that it can be used to generate the tag without resolving the super classes name
    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }
}
