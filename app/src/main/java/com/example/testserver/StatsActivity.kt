package com.example.testserver

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

class StatsActivity : AppCompatActivity() {
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        pieChart = findViewById(R.id.thingPieChart)
        barChart = findViewById(R.id.interactionBarChart)
        val uptimeText = findViewById<TextView>(R.id.uptimeText)
        uptimeText.text = "Uptime: ${ServientStats.uptimeSeconds()} s"
        setupPieChart()
        setupBarChart()
    }

    private fun setupPieChart() {
        val entries = ServientStats.requestCounts.map { (thing, count) ->
            PieEntry(count.toFloat(), thing)
        }

        val dataSet = PieDataSet(entries, "Accessi per Thing")
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS.toList())
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun setupBarChart() {
        val entries = ServientStats.interactionTypes.entries.mapIndexed { index, (type, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val labels = ServientStats.interactionTypes.keys.toList()
        val dataSet = BarDataSet(entries, "Tipo di Interazione")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.invalidate()
    }
}