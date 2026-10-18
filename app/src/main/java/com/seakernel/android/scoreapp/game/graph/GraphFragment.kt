package com.seakernel.android.scoreapp.game.graph

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.BarChart
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
import com.github.mikephil.charting.utils.Utils
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.databinding.FragmentGraphBinding
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.PlayerColors
import com.seakernel.android.scoreapp.utility.ScoreFormat
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationPadding
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.themeColor
import kotlin.math.cos
import kotlin.math.sin

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
        val gameColors = PlayerColors.forGame(requireContext(), game.settings.players)
        val scoreSets = scoreMap.map { scores ->
            val seat = game.settings.players.indexOfFirst { it.id == scores.key }.coerceAtLeast(0)
            val (color, isRepeated) = gameColors.getOrNull(seat) ?: PlayerColors.GameColor(colorOnSurface)
            LineDataSet(scores.value, game.settings.players.getOrNull(seat)?.name).also {
                setDataSetStyle(it, color, isDashed = isRepeated)
            }
        }

        // Finally set the chart data
        val data = LineData(scoreSets)
        data.setValueFormatter(graphEmptyFormatter)
        binding.gameChart.data = data
        binding.gameChart.invalidate()
        binding.gameChart.contentDescription = describeGraph(game, scoreMap)

        updatePodium(game, gameColors)
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
    private fun updatePodium(game: Game, gameColors: List<PlayerColors.GameColor>) {
        val players = game.settings.players
        val standings = players
            .map { player ->
                player to game.rounds.sumOf { round -> round.scores.find { it.player.id == player.id }?.value ?: 0.0 }
            }
            .sortedBy { (_, total) -> total }
            .let { if (game.settings.reversedScoring) it else it.reversed() }

        val entries = standings.mapIndexed { index, (_, total) -> BarEntry(index.toFloat(), total.toFloat()) }
        val colors = standings.map { (player, _) -> gameColors[players.indexOf(player)].color }
        val dataSet = BarDataSet(entries, null).apply {
            this.colors = colors
            valueTextSize = PODIUM_VALUE_SIZE
            valueTextColor = colorOnSurface
            valueFormatter = podiumValueFormatter
            isHighlightEnabled = false
        }

        binding.podiumChart.apply {
            val names = standings.map { (player, _) -> player.name }
            xAxis.valueFormatter = IndexAxisValueFormatter(names)
            xAxis.labelCount = standings.size
            // Bars grow from zero, so the axis always shows it. Past zero, the axis leaves room for the labels: above the
            // tallest bar (spaceTop), and below the lowest negative one (spaceBottom), both a share of the whole range
            axisLeft.apply {
                if (entries.any { it.y < 0f }) resetAxisMinimum() else axisMinimum = 0f
                if (entries.any { it.y > 0f }) resetAxisMaximum() else axisMaximum = 0f
            }
            data = BarData(dataSet)
            // The bars' widths are only known once the chart is laid out
            doOnLayout { fitPodiumLabels(names, standings.map { (_, total) -> ScoreFormat.format(total) }) }
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

    /**
     * Labels wider than their bar run into each other (a big game's names and totals became unreadable lines). When the
     * names don't fit side by side they're tilted, upright if even a tilt is too tight, and long ones are shortened, and
     * the totals get smaller until they fit. Screen readers get the full names from the chart's description.
     */
    private fun BarChart.fitPodiumLabels(names: List<String>, totals: List<String>) {
        val slot = viewPortHandler.contentWidth() / names.size.coerceAtLeast(1)
        val gap = Utils.convertDpToPixel(PODIUM_LABEL_GAP)
        // Each label is centered on its bar, so two neighbors touch once half of each fills a slot
        val widestPair = { widths: List<Int> -> widths.zipWithNext().maxOfOrNull { (a, b) -> (a + b) / 2f } ?: 0f }

        val valuePaint = Paint(renderer.paintValues).apply { textSize = Utils.convertDpToPixel(PODIUM_VALUE_SIZE) }
        val widestTotals = widestPair(totals.map { Utils.calcTextWidth(valuePaint, it) })
        val valueSize = if (widestTotals + gap <= slot) PODIUM_VALUE_SIZE else PODIUM_VALUE_SIZE * (slot - gap) / widestTotals
        data?.setValueTextSize(valueSize.coerceAtLeast(PODIUM_VALUE_MIN_SIZE))

        val paint = rendererXAxis.paintAxisLabels.apply { textSize = xAxis.textSize }
        val crowded = widestPair(names.map { Utils.calcTextWidth(paint, it) }) + gap > slot
        // A tilted name's box hangs from the axis, so a long one sits lower than a short neighbor, and their slanted lines
        // meet when the drop between them uses up the slot. Upright names don't depend on their lengths.
        val lineHeight = Utils.calcTextHeight(paint, "Ay")
        val tilt = Math.toRadians(-PODIUM_NAME_TILT.toDouble())
        val drops = names.map { Utils.calcTextWidth(paint, it.shortened()) * sin(tilt) + lineHeight * cos(tilt) }
        val tiltFits = drops.zipWithNext().all { (a, b) -> (slot + (b - a) / 2) * sin(tilt) >= lineHeight * PODIUM_NAME_LINES }
        val angle = when {
            !crowded -> 0f
            tiltFits -> PODIUM_NAME_TILT
            else -> -90f
        }
        xAxis.labelRotationAngle = angle
        xAxis.valueFormatter = IndexAxisValueFormatter(if (angle == 0f) names else names.map { it.shortened() })
        notifyDataSetChanged()
        invalidate()
    }

    private fun String.shortened(): String {
        if (length <= PODIUM_NAME_MAX_LENGTH) return this
        var end = PODIUM_NAME_MAX_LENGTH - 1
        if (Character.isHighSurrogate(this[end - 1])) end-- // Don't split an emoji
        return take(end).trimEnd() + "…"
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
            // The chart sizes the names' row by the text's height without descenders, cutting off the bottom of "y"
            extraBottomOffset = PODIUM_DESCENDER_SPACE
            xAxis.also {
                it.granularity = 1f
                it.position = XAxis.XAxisPosition.BOTTOM
                it.textColor = colorOnSurface
                it.textSize = 12f
                it.setDrawGridLines(false)
            }
            axisLeft.also {
                it.textColor = colorOnSurface
                // Room above the tallest bar and below the lowest negative one for their totals
                it.spaceTop = PODIUM_LABEL_SPACE
                it.spaceBottom = PODIUM_LABEL_SPACE
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
        private const val PODIUM_LABEL_SPACE = 15f // Percent of the axis's range
        private const val PODIUM_DESCENDER_SPACE = 4f // dp
        private const val PODIUM_LABEL_GAP = 8f // dp between neighboring names or totals
        private const val PODIUM_VALUE_SIZE = 14f // dp, the totals over the bars
        private const val PODIUM_VALUE_MIN_SIZE = 8f // dp, the smallest the totals get to fit
        private const val PODIUM_NAME_TILT = -45f // Degrees
        private const val PODIUM_NAME_LINES = 1.2f // Tilted names need this many line heights between them
        private const val PODIUM_NAME_MAX_LENGTH = 12 // Characters, once tilted

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
