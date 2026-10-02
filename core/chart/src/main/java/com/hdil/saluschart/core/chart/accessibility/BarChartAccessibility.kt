package com.hdil.saluschart.core.chart.accessibility

import com.hdil.saluschart.core.chart.ChartMark
import com.hdil.saluschart.core.chart.RangeChartMark
import com.hdil.saluschart.core.chart.StackedChartMark
import java.util.Locale

/**
 * Generates human-readable descriptions of bar-family chart data (plain, range, and stacked bars)
 * for accessibility services such as TalkBack, so that canvas-rendered charts remain
 * understandable to screen reader users.
 *
 * Kept free of Compose dependencies so it can be unit tested on the JVM without instrumentation.
 */
object BarChartAccessibility {

    /**
     * Summarizes an entire bar chart's data as a single spoken description.
     *
     * @param data The chart's marks. An empty list yields a "no data" description.
     * @param title Optional chart title; prefixed onto the description when present.
     * @param unit Optional unit suffix appended to values (e.g. "steps", "bpm").
     */
    fun describe(data: List<ChartMark>, title: String? = null, unit: String = ""): String {
        val titlePart = if (!title.isNullOrBlank()) "$title bar chart." else "Bar chart."

        if (data.isEmpty()) return "$titlePart No data."

        val values = data.map { it.y }
        val minValue = values.min()
        val maxValue = values.max()
        val last = data.last()

        val countPart = "${data.size} ${if (data.size == 1) "data point" else "data points"}."
        val rangePart = if (minValue == maxValue) {
            "Value: ${formatValue(maxValue)}${unitSuffix(unit)}."
        } else {
            "Range: ${formatValue(minValue)} to ${formatValue(maxValue)}${unitSuffix(unit)}."
        }
        val recentLabel = last.label ?: formatValue(last.x)
        val recentPart = "Most recent: $recentLabel, ${formatValue(last.y)}${unitSuffix(unit)}."

        return "$titlePart $countPart $rangePart $recentPart"
    }

    /**
     * Describes a single bar for per-element navigation (e.g. swiping between bars with
     * TalkBack). Includes the bar's position in the sequence so a user knows where they are.
     *
     * @param mark The bar's mark.
     * @param index Zero-based position of this bar within the chart.
     * @param total Total number of bars in the chart.
     * @param unit Optional unit suffix appended to the value.
     */
    fun describeBar(mark: ChartMark, index: Int, total: Int, unit: String = ""): String {
        val label = mark.label ?: formatValue(mark.x)
        val position = if (total > 1) "Bar ${index + 1} of $total. " else ""
        return "$position$label, ${formatValue(mark.y)}${unitSuffix(unit)}."
    }

    /**
     * Summarizes an entire range bar chart's data as a single spoken description.
     *
     * @param data The chart's marks. An empty list yields a "no data" description.
     * @param title Optional chart title; prefixed onto the description when present.
     * @param unit Optional unit suffix appended to values (e.g. "bpm").
     */
    fun describeRangeChart(data: List<RangeChartMark>, title: String? = null, unit: String = ""): String {
        val titlePart = if (!title.isNullOrBlank()) "$title range bar chart." else "Range bar chart."

        if (data.isEmpty()) return "$titlePart No data."

        val overallMin = data.minOf { it.minPoint.y }
        val overallMax = data.maxOf { it.maxPoint.y }
        val last = data.last()

        val countPart = "${data.size} ${if (data.size == 1) "data point" else "data points"}."
        val rangePart = if (overallMin == overallMax) {
            "Value: ${formatValue(overallMax)}${unitSuffix(unit)}."
        } else {
            "Range: ${formatValue(overallMin)} to ${formatValue(overallMax)}${unitSuffix(unit)}."
        }
        val recentLabel = last.label ?: formatValue(last.x)
        val recentValuePart = if (last.minPoint.y == last.maxPoint.y) {
            "${formatValue(last.maxPoint.y)}${unitSuffix(unit)}"
        } else {
            "${formatValue(last.minPoint.y)} to ${formatValue(last.maxPoint.y)}${unitSuffix(unit)}"
        }
        val recentPart = "Most recent: $recentLabel, $recentValuePart."

        return "$titlePart $countPart $rangePart $recentPart"
    }

    /**
     * Describes a single range bar for per-element navigation. Includes the bar's position in
     * the sequence so a user knows where they are.
     *
     * @param mark The bar's mark.
     * @param index Zero-based position of this bar within the chart.
     * @param total Total number of bars in the chart.
     * @param unit Optional unit suffix appended to the value.
     */
    fun describeRangeBar(mark: RangeChartMark, index: Int, total: Int, unit: String = ""): String {
        val label = mark.label ?: formatValue(mark.x)
        val position = if (total > 1) "Bar ${index + 1} of $total. " else ""
        val valuePart = if (mark.minPoint.y == mark.maxPoint.y) {
            "${formatValue(mark.maxPoint.y)}${unitSuffix(unit)}"
        } else {
            "${formatValue(mark.minPoint.y)} to ${formatValue(mark.maxPoint.y)}${unitSuffix(unit)}"
        }
        return "$position$label, $valuePart."
    }

    /**
     * Summarizes an entire stacked bar chart's data as a single spoken description, using each
     * bar's total (sum of its segments).
     *
     * @param data The chart's marks. An empty list yields a "no data" description.
     * @param title Optional chart title; prefixed onto the description when present.
     * @param unit Optional unit suffix appended to values (e.g. "kcal").
     */
    fun describeStackedChart(data: List<StackedChartMark>, title: String? = null, unit: String = ""): String {
        val titlePart = if (!title.isNullOrBlank()) "$title stacked bar chart." else "Stacked bar chart."

        if (data.isEmpty()) return "$titlePart No data."

        val totals = data.map { it.y }
        val minTotal = totals.min()
        val maxTotal = totals.max()
        val last = data.last()

        val countPart = "${data.size} ${if (data.size == 1) "data point" else "data points"}."
        val rangePart = if (minTotal == maxTotal) {
            "Value: ${formatValue(maxTotal)}${unitSuffix(unit)}."
        } else {
            "Range: ${formatValue(minTotal)} to ${formatValue(maxTotal)}${unitSuffix(unit)}."
        }
        val recentLabel = last.label ?: formatValue(last.x)
        val recentPart = "Most recent: $recentLabel, ${formatValue(last.y)}${unitSuffix(unit)}."

        return "$titlePart $countPart $rangePart $recentPart"
    }

    /**
     * Describes a single stacked bar for per-element navigation, as one composite description
     * covering every segment (there is only one focusable touch target per bar in a stacked
     * chart, unlike plain/range bars). Includes the bar's position in the sequence so a user
     * knows where they are.
     *
     * Segment labels come from each segment [ChartMark]'s own `label`, falling back to
     * "Segment N" (1-based) when absent *or* when it's identical to [mark]'s own label — a
     * common data-authoring mistake (labeling every segment with the bar's own category, e.g.
     * the day, instead of each segment's distinct identity) that would otherwise announce the
     * same word once per segment instead of anything meaningful.
     *
     * @param mark The bar's mark, with its segments in rendering order.
     * @param index Zero-based position of this bar within the chart.
     * @param total Total number of bars in the chart.
     * @param unit Optional unit suffix appended to each segment value and the total.
     */
    fun describeStackedBar(mark: StackedChartMark, index: Int, total: Int, unit: String = ""): String {
        val label = mark.label ?: formatValue(mark.x)
        val position = if (total > 1) "Bar ${index + 1} of $total. " else ""

        val segmentsPart = if (mark.segments.isEmpty()) {
            "No segments."
        } else {
            val segmentText = mark.segments.mapIndexed { i, segment ->
                val segmentLabel = segment.label?.takeIf { it != mark.label } ?: "Segment ${i + 1}"
                "$segmentLabel ${formatValue(segment.y)}"
            }.joinToString(", ")
            "$segmentText${unitSuffix(unit)}."
        }
        val totalPart = "Total ${formatValue(mark.y)}${unitSuffix(unit)}."

        return "$position$label. $segmentsPart $totalPart"
    }

    private fun unitSuffix(unit: String): String = if (unit.isBlank()) "" else " $unit"

    private fun formatValue(value: Double): String {
        return if (value == Math.floor(value) && !value.isInfinite()) {
            String.format(Locale.US, "%,d", value.toLong())
        } else {
            String.format(Locale.US, "%,.1f", value)
        }
    }
}
