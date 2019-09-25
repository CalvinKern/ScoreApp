package com.seakernel.android.scoreapp.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.FullGame
import kotlinx.android.synthetic.main.fragment_graph.*
import java.text.DecimalFormat

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

    private val modelObserver = Observer<FullGame?> { game -> game?.let { updateGraphData(it) } }

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
        graphToolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }

        viewModel.getGame().observe(this, modelObserver)
        viewModel.loadGame(
            arguments?.getLong(ARG_GAME_ID) ?: throw RuntimeException("No game ID provided to GraphFragment")
        )

        gameChart.setTouchEnabled(false)
        gameChart.setDrawBorders(true)
        gameChart.axisRight.isEnabled = false
        gameChart.description = Description().also {
            it.text = ""
        }
        gameChart.xAxis.also {
            it.granularity = 1f
            it.position = XAxis.XAxisPosition.BOTTOM
        }
        gameChart.axisLeft.also {
            it.granularity = 1f
            it.axisMinimum = 0f
        }
    }

    private fun updateGraphData(game: FullGame) {
        graphToolbar.title = game.settings.name

        // Get scores as a list of entries, separated by players
        val scoreMap =
            mapOf(*game.rounds.first().scores.map { Pair(it.player.id!!, mutableListOf<Entry>()) }.toTypedArray())
        game.rounds.forEach { round ->
            round.scores.forEach { score ->
                val playerList = scoreMap.getValue(score.player.id!!)
                playerList.add(Entry(round.number.toFloat() + 1, (playerList.lastOrNull()?.y ?: 0f) + score.value.toFloat()))
            }
        }

        // Convert the entries to line sets (with all that wonderful color info)
        val scaleFactor = 1.05f
        val scoreSets = scoreMap.map { scores ->
            val playerColor = ContextCompat.getColor(requireContext(), graphColors[scores.key.toInt() % graphColors.size])
            val playerName = game.settings.players.find { it.id == scores.key }?.name
            LineDataSet(scores.value, playerName).also {
                it.color = playerColor
                it.setCircleColor(playerColor)
                it.setDrawCircles(true)
                it.axisDependency = YAxis.AxisDependency.LEFT
                it.lineWidth = it.lineWidth * scaleFactor
                it.circleRadius = it.circleRadius * scaleFactor
                it.circleHoleRadius = it.circleHoleRadius * scaleFactor
                it.valueTextSize = 12f
                it.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return DecimalFormat("#.##").format(value)
                    }
                }
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