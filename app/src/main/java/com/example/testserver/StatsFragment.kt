package com.example.testserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.Sensor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
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
import io.ktor.client.request.request
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {
    private lateinit var uptimeText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var sensorSelector: Spinner
    private lateinit var resetButton: Button

    private var spinnerAdapter: ArrayAdapter<String>? = null
    private var currentSelectedProperty: String? = null

    private val preferencesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "PREFERENCES_UPDATED") {
                val updateType = intent.getStringExtra("update_type")
                if (updateType == "sensors") {
                    lifecycleScope.launch {
                        delay(2000)
                        updateSensorSpinner()
                        updateCharts()
                    }
                }
            }
        }
    }

    private fun getFriendlyNameProperty(propertyName: String): String {
        val parts = propertyName.split("-")
        if (parts.size < 2) return propertyName

        val sensorTypeStr = parts.last().split("_")[0]
        val axis = if (propertyName.contains("_")) {
            " (${propertyName.substringAfterLast("_").uppercase()})"
        } else ""

        val sensorType = sensorTypeStr.toIntOrNull() ?: return propertyName

        val baseName = when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometro"
            Sensor.TYPE_LIGHT -> "Sensore di luminosità"
            Sensor.TYPE_GYROSCOPE -> "Giroscopio"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometro"
            Sensor.TYPE_PRESSURE -> "Barometro"
            Sensor.TYPE_PROXIMITY -> "Sensore di prossimità"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Sensore temperatura ambiente"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Sensore umidità"
            Sensor.TYPE_GRAVITY -> "Sensore gravità"
            else -> propertyName
        }

        return baseName + axis
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ServientStats.initialize(requireContext())

        uptimeText = view.findViewById(R.id.uptimeText)
        pieChart = view.findViewById(R.id.thingPieChart)
        barChart = view.findViewById(R.id.interactionBarChart)
        sensorSelector = view.findViewById(R.id.sensorSelector)
        lineChart = view.findViewById(R.id.sensorLineChart)

        updateUptimeText()
        setupPieChart()
        setupBarChart()
        setupSensorSelector()

        val filter = IntentFilter("PREFERENCES_UPDATED")
        requireContext().registerReceiver(preferencesReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        startPeriodicUpdates()

        resetButton = view.findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            ServientStats.reset()
            updateAllUI()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(preferencesReceiver)
        } catch (e: Exception) {
            // Ignora..
        }
    }

    private fun updateUptimeText() {
        uptimeText.text = "Uptime: ${ServientStats.uptimeSeconds()} s"
    }

    private var hasAnimatedPieChart = false

    private fun setupPieChart() {
        val affordances = getAffordanceStatistics()

        if (affordances.isEmpty()) {
            pieChart.data = null
            pieChart.centerText = "Nessun dato disponibile"
            pieChart.setEntryLabelColor(Color.DKGRAY)
            pieChart.setCenterTextSize(14f)
            pieChart.invalidate()
            return
        }

        val entries = affordances.map { (affordance, count) ->
            PieEntry(count.toFloat(), cleanAffordanceName(affordance))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"), // Verde
            Color.parseColor("#2196F3"), // Blu
            Color.parseColor("#FFC107"), // Giallo
            Color.parseColor("#FF5722"), // Arancione
            Color.parseColor("#9C27B0")  // Viola
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 13f
        dataSet.sliceSpace = 2f

        val data = PieData(dataSet)

        with(pieChart) {
            this.data = data
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.DKGRAY)
            centerText = "Distribuzione Accessi"
            setCenterTextSize(16f)
            description.isEnabled = false
            legend.isEnabled = false
            if (!hasAnimatedPieChart) {
                pieChart.animateY(800, Easing.EaseInOutQuad)
                hasAnimatedPieChart = true
            }
        }
    }

    private var hasAnimatedBarChart = false
    private fun setupBarChart() {
        if (ServientStats.interactionTypes.isEmpty()) {
            barChart.data = null
            barChart.invalidate()
            return
        }

        val entries = ServientStats.interactionTypes.entries.mapIndexed { index, (type, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        val labels = ServientStats.interactionTypes.keys.toList()

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#03A9F4"), // Azzurro
            Color.parseColor("#FF9800"), // Arancione
            Color.parseColor("#8BC34A"), // Verde chiaro
            Color.parseColor("#E91E63")  // Rosa
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.DKGRAY

        val data = BarData(dataSet)
        data.barWidth = 0.5f

        with(barChart) {
            this.data = data
            description.isEnabled = false
            setFitBars(true)
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.textColor = Color.DKGRAY
            xAxis.labelRotationAngle = -30f
            xAxis.textColor = Color.DKGRAY
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            legend.isEnabled = false
            if (!hasAnimatedBarChart) {
                barChart.animateY(1000)
                hasAnimatedBarChart = true
            }
        }
    }


    private fun setupSensorSelector() {
        updateSensorSpinner()
    }

    private fun updateSensorSpinner() {
        val properties = SensorDataHolder.getAllProperties()
            .filterNot { it.contains("photo") || it.contains("audio") }
            .sorted()

        if (properties.isEmpty()) {
            val emptyList = listOf("Nessun sensore disponibile")
            spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, emptyList)
            spinnerAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sensorSelector.adapter = spinnerAdapter
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val friendlyToTecnhnicalMap = mutableMapOf<String, String>()
        val friendlyNames = properties.map { property ->
            val friendlyName = getFriendlyNameProperty(property)
            friendlyToTecnhnicalMap[friendlyName] = property
            friendlyName
        }.sorted()

        val previousSelection = currentSelectedProperty
        val previousFriendlyName = if (previousSelection != null) {
            getFriendlyNameProperty(previousSelection)
        } else null

        spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, friendlyNames)
        spinnerAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sensorSelector.adapter = spinnerAdapter

        val positionToSelect = if (previousFriendlyName != null && friendlyNames.contains(previousFriendlyName)) {
            friendlyNames.indexOf(previousFriendlyName)
        } else {
            0
        }

        sensorSelector.setSelection(positionToSelect)
        currentSelectedProperty = friendlyToTecnhnicalMap[friendlyNames.getOrNull(positionToSelect)]

        sensorSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position < friendlyNames.size) {
                    val selectedFriendlyName = friendlyNames[position]
                    val selectedProperty = friendlyToTecnhnicalMap[selectedFriendlyName]
                    if (selectedProperty != null) {
                        currentSelectedProperty = selectedProperty
                        setupLineChart(selectedProperty, selectedFriendlyName)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Niente..
            }
        }

        currentSelectedProperty?.let {
            val friendlyName = getFriendlyNameProperty(it)
            setupLineChart(it, friendlyName)
        }
    }

    private fun setupLineChart(property: String, friendlyName: String = property) {
        val history = SensorDataHolder.getHistory(property)
        if (history.isEmpty()) {
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val entries = history.mapIndexed { index, (_, value) ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, friendlyName)
        dataSet.color = Color.parseColor("#3F51B5")
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)
        dataSet.setCircleColor(Color.parseColor("#3F51B5"))
        dataSet.circleRadius = 3f
        dataSet.lineWidth = 2f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val data = LineData(dataSet)

        with(lineChart) {
            this.data = data
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.textColor = Color.DKGRAY
            xAxis.textColor = Color.DKGRAY
            xAxis.setDrawGridLines(false)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            legend.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun startPeriodicUpdates() {
        lifecycleScope.launch {
            while (isAdded) {
                delay(5000) // Aggiorna ogni 5 secondi
                updateUptimeText()
                updateSensorSpinner()
                if (ServientStats.totalRequests > 0) {
                    updateCharts()
                }
            }
        }
    }

    private fun updateCharts() {
        setupPieChart()
        setupBarChart()
    }

    private fun updateAllUI() {
        updateUptimeText()
        updateCharts()
        updateSensorSpinner()
    }

    private fun getSafeRequestCount(): Map<String, Int> {
        return if (ServientStats.requestCounts.isEmpty()) {
            mapOf("Default" to 5)
        } else {
            ServientStats.requestCounts
        }
    }

    private fun getAffordanceStatistics(): Map<String, Int> {
        val affordanceRequests = ServientStats.getAffordanceStatistics()

        if (affordanceRequests.isEmpty()) {
            return mapOf("Nessun accesso" to 1)
        }
        if (affordanceRequests.isEmpty() && ServientStats.requestCounts.containsKey("smartphone")) {
            val smartphoneRequests = ServientStats.requestCounts["smartphone"] ?: 0
            val enabledSensors = SensorDataHolder.getAllProperties()
            if (enabledSensors.isNotEmpty()) {
                //TODO: Rimuovere Distribuisci accessi tra i sensori abilitati per una visuale più interessante?
                val accessPerSensor = smartphoneRequests / enabledSensors.size
                val remainder = smartphoneRequests % enabledSensors.size

                return enabledSensors.mapIndexed { index, sensor ->
                    val extraAccess = if (index < remainder) 1 else 0
                    val friendlyName = getFriendlyNameProperty(sensor)
                    friendlyName to (accessPerSensor + extraAccess)
                }.toMap()
            }
        }
        return affordanceRequests.mapKeys { (key, _) ->
            if (key.startsWith("smartphone.")) {
                val propertyName = key.removePrefix("smartphone.")
                getFriendlyNameProperty(propertyName)
            } else {
                cleanAffordanceName(key)
            }
        }
    }

    private fun cleanAffordanceName(affordanceName: String): String {
        return affordanceName
            .removePrefix("smartphone.")
            .replace("-", " ")
            .split("_")
            .first()
            .replaceFirstChar { it.uppercase() }
    }
}