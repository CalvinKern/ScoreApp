package com.seakernel.android.scoreapp.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.FullGame
import kotlinx.android.synthetic.main.fragment_graph.*

class GraphFragment : Fragment() {

    private val graphColors = intArrayOf(
        R.color.graphRed,
        R.color.graphPurple,
        R.color.graphBlue,
        R.color.graphCyan,
        R.color.graphTeal,
        R.color.graphGreen,
        R.color.graphYellow,
        R.color.graphOrange,
        R.color.graphBrown,
        R.color.graphGray
    )

    private val modelObserver = Observer<FullGame?> { game -> game?.let { showGraphs(it) } }

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.getGame().removeObserver(modelObserver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getGame().observe(this, modelObserver)
        viewModel.loadGame(
            arguments?.getLong(ARG_GAME_ID) ?: throw RuntimeException("No game ID provided to GraphFragment")
        )
    }

    private fun showGraphs(game: FullGame) {
        graphToolbar.title = game.settings.name

        // Get scores as a list of entries, separated by players
        val scoreMap =
            mapOf(*game.rounds.first().scores.map { Pair(it.player.id!!, mutableListOf<Entry>()) }.toTypedArray())
        game.rounds.forEach { round ->
            round.scores.forEach { score ->
                val playerList = scoreMap.getValue(score.player.id!!)
                playerList.add(Entry(round.number.toFloat(), (playerList.lastOrNull()?.y ?: 0f) + score.value.toFloat()))
            }
        }

        // Convert the entries to line sets (with all that wonderful color info)
        val scoreSets = scoreMap.map { scores ->
            val playerName = game.settings.players.find { it.id == scores.key }?.name
            LineDataSet(scores.value, playerName).apply {
                color = graphColors[scores.key.toInt() % graphColors.size]
                valueTextColor = color
                setCircleColor(color)
                setDrawCircles(true)
                axisDependency = YAxis.AxisDependency.LEFT
            }
        }

        // Finally set the chart data
        val data = LineData(scoreSets)
        gameChart.data = data
        gameChart.invalidate()
    }

    companion object {
        private const val ARG_GAME_ID = "game_id"

        fun newInstance(gameId: Long): GraphFragment {
            return GraphFragment().also { fragment ->
                fragment.arguments = Bundle().apply {
                    putLong(ARG_GAME_ID, gameId)
                }
            }
        }
    }
}