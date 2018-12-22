package com.seakernel.android.scoreapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seakernel.android.scoreapp.game.GameFragment
import com.seakernel.android.scoreapp.gamecreate.GameCreateFragment
import com.seakernel.android.scoreapp.gamelist.GameListFragment

class MainActivity : AppCompatActivity(), GameListFragment.GameListListener, GameCreateFragment.GameCreateListener {

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

    override fun onShowGameScreen(gameId: Long) {
        supportFragmentManager.findFragmentByTag(GameCreateFragment.FRAGMENT_TAG)?.let {
            supportFragmentManager.popBackStack() // Get rid of create fragment if it exists
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, GameFragment.newInstance(gameId), GameFragment.FRAGMENT_TAG)
            .addToBackStack(GameFragment.FRAGMENT_TAG)
            .commit()
    }

    override fun onShowCreateGameScreen() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, GameCreateFragment.newInstance(), GameCreateFragment.FRAGMENT_TAG)
            .addToBackStack(GameCreateFragment.FRAGMENT_TAG)
            .commit()
    }
}
