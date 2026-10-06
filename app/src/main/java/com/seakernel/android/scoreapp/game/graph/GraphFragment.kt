package com.seakernel.android.scoreapp.game.graph

import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.data.Game
import com.seakernel.android.scoreapp.databinding.FragmentGraphBinding
import com.seakernel.android.scoreapp.utility.AnalyticsConstants
import com.seakernel.android.scoreapp.utility.ScoreFormat
import com.seakernel.android.scoreapp.utility.applyWindowInsetsCutout
import com.seakernel.android.scoreapp.utility.applyWindowInsetsNavigationMargin
import com.seakernel.android.scoreapp.utility.logScreenView
import com.seakernel.android.scoreapp.utility.themeColor

class GraphFragment : Fragment() {

    private val colorOnSurface by lazy { requireContext().themeColor(R.attr.colorOnSurface) }

    /**
     * Players get colors by their seat in the game, so everyone in a game of up to 10 players has their own color
     * and keeps it while the game goes on. Larger games repeat the colors, with dashed lines to tell them apart.
     */
    private val graphColors = intArrayOf(
        R.color.graphRed,
        R.color.graphBlue,
        R.color.graphGreen,
        R.color.graphOrange,
        R.color.graphPurple,
        R.color.graphCyan,
        R.color.graphBrown,
        R.color.graphYellow,
        R.color.graphGray,
        R.color.graphPink,
    )

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
        binding.gameChart.applyWindowInsetsNavigationMargin()

        viewModel.getGame().observe(viewLifecycleOwner, modelObserver)
        viewModel.loadGame(
            arguments?.getLong(ARG_GAME_ID)
                ?: throw RuntimeException("No game ID provided to GraphFragment")
        )

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
            val playerName = game.settings.players.getOrNull(seat)?.name
            val playerColor = graphColors[seat % graphColors.size]
            LineDataSet(scores.value, playerName).also {
                setDataSetStyle(it, getColor(requireContext(), playerColor), isDashed = seat >= graphColors.size)
            }
        }

        // Finally set the chart data
        val data = LineData(scoreSets)
        data.setValueFormatter(graphEmptyFormatter)
        binding.gameChart.data = data
        binding.gameChart.invalidate()
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
