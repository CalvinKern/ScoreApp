package com.seakernel.android.scoreapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.game.GameFragment
import com.seakernel.android.scoreapp.gamecreate.PlayerSelectFragment
import com.seakernel.android.scoreapp.gamelist.GameListFragment
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), GameListFragment.GameListListener, PlayerSelectFragment.PlayerSelectListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add the list fragment if we don't have any state
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, GameListFragment.newInstance())
                .commit()
        }
    }

    override fun onPlayersSelected(playerIds: List<Long>) {
        popBackStackIfFound(PlayerSelectFragment::class)
        // TODO: Find the create game fragment and add new players
//        supportFragmentManager.findFragmentByTag()
    }

    fun onShowPlayerSelectScreen(playerIds: List<Long>) {
        showFragment(PlayerSelectFragment.newInstance(playerIds), PlayerSelectFragment::class.java.simpleName)
    }

    override fun onShowGameScreen(gameId: Long) {
        popBackStackIfFound(PlayerSelectFragment::class)
        showFragment(GameFragment.newInstance(gameId), GameFragment::class.java.simpleName)
    }

    override fun onShowCreateGameScreen() {
        TODO("Create this screen")
    }

    // Helper Functions
    private fun popBackStackIfFound(clazz: KClass<*>) {
        supportFragmentManager.findFragmentByTag(clazz.java.simpleName)?.let {
            supportFragmentManager.popBackStack() // Get rid of create fragment if it exists
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }
}
