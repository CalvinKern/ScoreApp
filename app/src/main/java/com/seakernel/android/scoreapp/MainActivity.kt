package com.seakernel.android.scoreapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seakernel.android.scoreapp.gamecreate.GameCreateFragment
import com.seakernel.android.scoreapp.gamelist.GameListFragment

class MainActivity : AppCompatActivity(), GameListFragment.GameListListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add the list fragment if we don't have any state
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, GameListFragment.newInstance(), "gameList")
                .commit()
        }
    }

    override fun onShowGameScreen(gameId: Long) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onShowCreateGameScreen() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, GameCreateFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }
}
