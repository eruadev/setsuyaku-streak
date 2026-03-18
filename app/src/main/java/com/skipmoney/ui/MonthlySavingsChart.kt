package com.skipmoney.ui

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.skipmoney.R
import java.time.LocalDate

@Composable
fun MonthlySavingsChart(
    title: String,
    hint: String,
    values: List<Float>,
    dayLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (values.all { it <= 0f }) {
                InlineEmptyState(
                    title = androidx.compose.ui.res.stringResource(R.string.monthly_savings_chart_empty_title),
                    body = androidx.compose.ui.res.stringResource(R.string.monthly_savings_chart_empty_body),
                )
                return@Column
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                factory = { context ->
                    BarChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setDrawGridBackground(false)
                        setDrawBarShadow(false)
                        setFitBars(true)
                        setScaleEnabled(false)
                        isDoubleTapToZoomEnabled = false
                        setPinchZoom(false)
                        setExtraOffsets(4f, 4f, 4f, 8f)

                        axisRight.isEnabled = false
                        axisLeft.apply {
                            setTextColor(axisColor)
                            gridColor = Color.TRANSPARENT
                            axisMinimum = 0f
                            setDrawAxisLine(false)
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String =
                                    if (value == 0f) "0" else "¥${value.toInt()}"
                            }
                        }

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setTextColor(axisColor)
                            gridColor = Color.TRANSPARENT
                            setDrawAxisLine(false)
                            granularity = 1f
                            labelRotationAngle = 0f
                        }
                    }
                },
                update = { chart ->
                    val todayDayOfMonth = LocalDate.now().dayOfMonth
                    // Root cause note:
                    // The recent list is a normal Compose list, so new state was reflected
                    // immediately. This chart uses AndroidView, and MPAndroidChart could keep
                    // drawing its previous internal dataset unless we both push fresh series data
                    // and force the view to refresh/recreate when chart state changes.
                    Log.d(
                        "MonthlySavingsChart",
                        "input: valuesCount=${values.size}, nonZeroCount=${values.count { it > 0f }}, labelsRange=${dayLabels.firstOrNull()}..${dayLabels.lastOrNull()} (values are yen)",
                    )
                    val entries = values.mapIndexed { index, amount ->
                        // The chart must render yen values, not cents.
                        BarEntry((index + 1).toFloat(), amount)
                    }
                    val dataSet = BarDataSet(entries, title).apply {
                        color = primaryColor
                        setDrawValues(false)
                        highLightAlpha = 0
                    }

                    chart.xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val day = value.toInt()
                            return if (day in 1..dayLabels.size) dayLabels[day - 1] else ""
                        }
                    }
                    chart.xAxis.axisMinimum = 0.5f
                    chart.xAxis.axisMaximum = dayLabels.size + 0.5f
                    // MPAndroidChart can keep a stale Y-axis max from the previous dataset,
                    // which makes newly added small yen values effectively invisible.
                    chart.axisLeft.resetAxisMaximum()
                    chart.axisLeft.resetAxisMinimum()
                    chart.axisLeft.axisMinimum = 0f
                    chart.axisLeft.axisMaximum = values.maxOrNull() ?: 0f
                    chart.data = BarData(dataSet).apply {
                        barWidth = 0.78f
                    }
                    val entryDebugText = entries.joinToString { entry ->
                        "(x=${entry.x}, y=${entry.y})"
                    }
                    Log.d(
                        "MonthlySavingsChart",
                        "dataset: entryCount=${entries.size}, entriesYen=$entryDebugText",
                    )
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.setFitBars(true)
                    // Keep the initial viewport at the start of the month. The previous
                    // implementation forced a 7-day window and jumped to the tail end, which
                    // hid today's bar until the user scrolled back manually.
                    chart.fitScreen()
                    chart.setVisibleXRangeMaximum(dayLabels.size.toFloat())
                    chart.moveViewToX(1f)
                    chart.highlightValues(null)
                    Log.d(
                        "MonthlySavingsChart",
                        "render: today=$todayDayOfMonth, axisMinimum=${chart.axisLeft.axisMinimum}, axisMaximum=${chart.axisLeft.axisMaximum}, visibleXRange=${chart.lowestVisibleX}..${chart.highestVisibleX}, xAxisMin=${chart.xAxis.axisMinimum}, xAxisMax=${chart.xAxis.axisMaximum}",
                    )
                    chart.invalidate()
                },
            )
        }
    }
}
