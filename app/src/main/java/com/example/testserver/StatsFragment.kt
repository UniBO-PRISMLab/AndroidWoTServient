package com.example.testserver

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class StatsFragment : Fragment() {
    private lateinit var uptimeText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var sensorSelector: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uptimeText = view.findViewById(R.id.uptimeText)
        pieChart = view.findViewById(R.id.thingPieChart)
        barChart = view.findViewById(R.id.interactionBarChart)
        sensorSelector = view.findViewById(R.id.sensorSelector)
        lineChart = view.findViewById(R.id.sensorLineChart)

        uptimeText.text = "Uptime: ${ServientStats.uptimeSeconds()} s"
        setupPieChart()
        setupBarChart()
        setupSensorSelector()
    }

    private fun setupPieChart() {
        val entries = getSafeRequestCount().map { (thing, count) ->
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
        barChart.xAxis.labelRotationAngle = -45f
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

    private fun setupSensorSelector() {
        val properties = SensorDataHolder.getAllProperties().toList()
        if (properties.isEmpty()) return

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, properties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sensorSelector.adapter = adapter

        sensorSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedProperty = properties[position]
                setupLineChart(selectedProperty)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Non fare niente
            }
        }

        setupLineChart(properties.first())
    }

    private fun setupLineChart(property: String) {
        val history = SensorDataHolder.getHistory(property)
        if (history.isEmpty()) return
        val entries = history.mapIndexed { index, (_, value) ->
            Entry(index.toFloat(), value)
        }
        val dataSet = LineDataSet(entries, property)
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }

    private fun getSafeRequestCount(): Map<String, Int> {
        return if (ServientStats.requestCounts.isEmpty()) {
            mapOf("Default" to 5)
        } else {
            ServientStats.requestCounts
        }
    }
}