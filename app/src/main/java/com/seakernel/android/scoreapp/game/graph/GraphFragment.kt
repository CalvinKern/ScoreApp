package com.seakernel.android.scoreapp.game.graph

import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.databinding.FragmentGraphBinding
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.PlayerColors
import com.seakernel.android.scoreapp.utility.ScoreFormat
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.themeColor

class GraphFragment : Fragment() {

    private val colorOnSurface by lazy { requireContext().themeColor(R.attr.colorOnSurface) }

    private val modelObserver = Observer<Game?> { game -> game?.let { updateGraphData(it) } }

    private val viewModel: GraphViewModel by viewModels<GraphViewModel>()

    private var _binding: FragmentGraphBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.getGame().removeObserver(modelObserver)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.graphToolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.graphAppBar.applyWindowInsetsCutout()
        binding.graphContainer.applyWindowInsetsNavigationPadding()

        viewModel.getGame().observe(viewLifecycleOwner, modelObserver)
        viewModel.loadGame(
            arguments?.getLong(ARG_GAME_ID)
                ?: throw RuntimeException("No game ID provided to GraphFragment")
        )

        initPodiumStyle()
        initChartStyle()
    }

    override fun onResume() {
        super.onResume()
        logScreenView(AnalyticsConstants.ScreenName.GraphFragment)
    }

    private fun updateGraphData(game: Game) {
        binding.graphToolbar.title = game.settings.name
        binding.graphToolbar.setSubtitle(R.string.graphSubtitle) // Update the subtitle with the title to avoid subtitle flashing in first

        // Get scores as a list of entries, separated by players
        val scoreMap =
            mapOf(*(game.rounds.firstOrNull()?.scores?.map {
                Pair(
                    it.player.id!!,
                    mutableListOf<Entry>()
                )
            }?.toTypedArray() ?: arrayOf()))
        game.rounds.forEachIndexed { roundIndex, round ->
            round.scores.forEach { score ->
                if (score.player.id != null && scoreMap.containsKey(score.player.id))
                    scoreMap.getValue(score.player.id).also { playerList ->
                        playerList.add(
                            Entry(
                                roundIndex.toFloat() + 1, // Labeled by position, the same as everywhere else
                                (playerList.lastOrNull()?.y ?: 0f) + score.value.toFloat()
                            )
                        )
                    }
            }
        }

        // Convert the entries to line sets (with all that wonderful color info)
        val scoreSets = scoreMap.map { scores ->
            val seat = game.settings.players.indexOfFirst { it.id == scores.key }.coerceAtLeast(0)
            val player = game.settings.players.getOrNull(seat) ?: Player()
            LineDataSet(scores.value, player.name).also {
                setDataSetStyle(
                    it,
                    PlayerColors.forPlayer(requireContext(), player, seat),
                    isDashed = PlayerColors.isRepeated(player, seat),
                )
            }
        }

        // Finally set the chart data
        val data = LineData(scoreSets)
        data.setValueFormatter(graphEmptyFormatter)
        binding.gameChart.data = data
        binding.gameChart.invalidate()
        binding.gameChart.contentDescription = describeGraph(game, scoreMap)

        updatePodium(game)
    }

    /** The chart is only drawn, so screen readers get each player's running total after every round instead */
    private fun describeGraph(game: Game, scoreMap: Map<Long, List<Entry>>): String {
        val runningTotals = game.settings.players.joinToString(". ") { player ->
            val totals = player.id?.let { scoreMap[it] }.orEmpty().joinToString(", ") { ScoreFormat.format(it.y.toDouble()) }
            getString(R.string.graphPlayerRoundsDescription, player.name, totals)
        }
        return getString(R.string.graphDescription, describeRounds(game), runningTotals)
    }

    private fun describeRounds(game: Game) =
        resources.getQuantityString(R.plurals.roundsHolder, game.rounds.size, game.rounds.size)

    /** Every player's total as a bar, best standing first, in the same colors as the rounds graph */
    private fun updatePodium(game: Game) {
        val players = game.settings.players
        val standings = players
            .map { player ->
                player to game.rounds.sumOf { round -> round.scores.find { it.player.id == player.id }?.value ?: 0.0 }
            }
            .sortedBy { (_, total) -> total }
            .let { if (game.settings.reversedScoring) it else it.reversed() }

        val entries = standings.mapIndexed { index, (_, total) -> BarEntry(index.toFloat(), total.toFloat()) }
        val colors = standings.map { (player, _) ->
            PlayerColors.forPlayer(requireContext(), player, players.indexOf(player).coerceAtLeast(0))
        }
        val dataSet = BarDataSet(entries, null).apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = colorOnSurface
            valueFormatter = podiumValueFormatter
            isHighlightEnabled = false
        }

        binding.podiumChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(standings.map { (player, _) -> player.name })
            xAxis.labelCount = standings.size
            // Bars grow from zero, leaving room below any negative totals for their labels
            axisLeft.axisMinimum = minOf(0f, entries.minOfOrNull { it.y }?.times(PODIUM_NEGATIVE_SPACE) ?: 0f)
            data = BarData(dataSet)
            invalidate()
            contentDescription = getString(
                R.string.podiumDescription,
                describeRounds(game),
                standings.joinToString(". ") { (player, total) ->
                    getString(R.string.playerTotalDescription, player.name, ScoreFormat.format(total))
                },
            )
        }
    }

    private fun initPodiumStyle() {
        binding.podiumChart.apply {
            axisRight.isEnabled = false
            legend.isEnabled = false
            description = Description().also {
                it.text = ""
            }
            setTouchEnabled(false)
            setFitBars(true)
            xAxis.also {
                it.granularity = 1f
                it.position = XAxis.XAxisPosition.BOTTOM
                it.textColor = colorOnSurface
                it.textSize = 12f
                it.setDrawGridLines(false)
            }
            axisLeft.also {
                it.textColor = colorOnSurface
                // Room above the tallest bar for its total
                it.spaceTop = 15f
            }
        }
    }

    private fun initChartStyle() {
        binding.gameChart.apply {
            axisRight.isEnabled = false
            isDoubleTapToZoomEnabled = false
            description = Description().also {
                it.text = ""
            }
            legend.also {
                it.textColor = colorOnSurface
                // Lines (instead of squares) so the legend shows which players have dashed lines
                it.form = Legend.LegendForm.LINE
                it.formSize = 16f
                it.formLineWidth = 4f
                // Wrap onto more lines for larger games instead of running off the screen
                it.isWordWrapEnabled = true
            }
            xAxis.also {
                it.granularity = 1f
                it.position = XAxis.XAxisPosition.BOTTOM
                it.textColor = colorOnSurface
            }
            axisLeft.also {
                it.granularity = 1f
                it.textColor = colorOnSurface
            }
            setDrawBorders(true)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    binding.gameChart.data.setValueFormatter(graphEmptyFormatter)
                }

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        binding.gameChart.data.getDataSetForEntry(it).apply {
                            binding.gameChart.data.setValueFormatter(graphEmptyFormatter)
                            valueFormatter =
                                graphValueFormatter
                        }
                    }
                }

            })
        }
    }

    private fun setDataSetStyle(dataSet: LineDataSet, playerColor: Int, isDashed: Boolean) {
        dataSet.apply { // Random float values are defined by dp
            color = playerColor
            lineWidth = 4f
            if (isDashed) {
                enableDashedLine(DASH_LENGTH, DASH_LENGTH, 0f)
                formLineDashEffect = DashPathEffect(floatArrayOf(DASH_LENGTH, DASH_LENGTH), 0f)
            }
            valueTextSize = 14f
            axisDependency = YAxis.AxisDependency.LEFT
            valueTextColor = ColorUtils.blendARGB(playerColor, colorOnSurface, 0.3f)
            setDrawHighlightIndicators(false)

            // Set circle data
            circleRadius = 4f
            circleHoleRadius = 4f
            setDrawCircles(true)
            setCircleColor(playerColor)
        }
    }

    companion object {
        private const val ARG_GAME_ID = "game_id"
        private const val DASH_LENGTH = 16f
        private const val PODIUM_NEGATIVE_SPACE = 1.15f

        private val podiumValueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = ScoreFormat.format(value.toDouble())
        }

        private val graphEmptyFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = ""
        }

        private val graphValueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return if ((entry?.x?.toInt() ?: 0) % 2 != 0) {
                    ""
                } else {
                    super.getPointLabel(entry)
                }
            }

            override fun getFormattedValue(value: Float): String {
                return ScoreFormat.format(value.toDouble())
            }
        }

        fun newInstance(gameId: Long): GraphFragment {
            return GraphFragment().also { fragment ->
                fragment.arguments = Bundle().apply {
                    putLong(ARG_GAME_ID, gameId)
                }
            }
        }
    }
}
