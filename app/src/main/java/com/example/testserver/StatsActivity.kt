package com.example.testserver

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class StatsActivity : BaseActivity() {
    private lateinit var uptimeText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_with_nav)

        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        val statsLayout = layoutInflater.inflate(R.layout.activity_stats, contentFrame, false)
        contentFrame.addView(statsLayout)
        initializeViews(statsLayout)
        setupBottomNavigation(R.id.nav_stats)

        setupPieChart()
        setupBarChart()
    }

    private fun initializeViews(rootView: View) {
        uptimeText = rootView.findViewById(R.id.uptimeText)
        pieChart = rootView.findViewById(R.id.thingPieChart)
        barChart = rootView.findViewById(R.id.interactionBarChart)
        uptimeText.text = "Uptime: ${ServientStats.uptimeSeconds()} s"
    }

    private fun setupPieChart() {
        val entries = getSafeRequestCount().map {(thing, count) ->
            PieEntry(count.toFloat(), thing)
        }

        pieChart.description.isEnabled = false
        val dataSet = PieDataSet(entries, "Accessi per Thing")
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS.toList())
        val data = PieData(dataSet)
        data.setDrawValues(true)
        data.setValueTextSize(12f)
        data.setValueTextColor(Color.WHITE)
        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.centerText = "Distribuzione Accessi"
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupBarChart() {
        val entries = ServientStats.interactionTypes.entries.mapIndexed { index, (type, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val labels = ServientStats.interactionTypes.keys.toList()
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f
        barChart.xAxis.labelRotationAngle = -45f // evitare sovrapposizioni
        barChart.legend.isEnabled = true
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        val dataSet = BarDataSet(entries, "Tipo di Interazione")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        val data = BarData(dataSet)
        data.setValueTextSize(12f)
        barChart.data = data
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun getSafeRequestCount(): Map<String, Int> {
        return if (ServientStats.requestCounts.isEmpty()) {
            mapOf("Default" to 5)
        } else {
            ServientStats.requestCounts
        }
    }
}