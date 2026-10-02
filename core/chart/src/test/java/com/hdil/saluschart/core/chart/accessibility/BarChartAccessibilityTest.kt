package com.hdil.saluschart.core.chart.accessibility

import com.hdil.saluschart.core.chart.ChartMark
import com.hdil.saluschart.core.chart.RangeChartMark
import org.junit.Assert.assertEquals
import org.junit.Test

class BarChartAccessibilityTest {

    @Test
    fun `describe summarizes title, count, range, and most recent point`() {
        val data = listOf(
            ChartMark(x = 0.0, y = 4201.0, label = "Mon"),
            ChartMark(x = 1.0, y = 6201.0, label = "Tue"),
            ChartMark(x = 2.0, y = 12847.0, label = "Wed"),
        )

        val description = BarChartAccessibility.describe(data, title = "Step count", unit = "steps")

        assertEquals(
            "Step count bar chart. 3 data points. Range: 4,201 to 12,847 steps. Most recent: Wed, 12,847 steps.",
            description
        )
    }

    @Test
    fun `describe omits title prefix when title is null or blank`() {
        val data = listOf(ChartMark(x = 0.0, y = 5.0, label = "Mon"))

        assertEquals(
            "Bar chart. 1 data point. Value: 5. Most recent: Mon, 5.",
            BarChartAccessibility.describe(data, title = null)
        )
        assertEquals(
            "Bar chart. 1 data point. Value: 5. Most recent: Mon, 5.",
            BarChartAccessibility.describe(data, title = "  ")
        )
    }

    @Test
    fun `describe uses Value phrasing when every bar is equal`() {
        val data = listOf(
            ChartMark(x = 0.0, y = 10.0, label = "Mon"),
            ChartMark(x = 1.0, y = 10.0, label = "Tue"),
        )

        val description = BarChartAccessibility.describe(data, unit = "kg")

        assertEquals("Bar chart. 2 data points. Value: 10 kg. Most recent: Tue, 10 kg.", description)
    }

    @Test
    fun `describe falls back to x value when label is missing`() {
        val data = listOf(ChartMark(x = 3.0, y = 7.5))

        val description = BarChartAccessibility.describe(data)

        assertEquals("Bar chart. 1 data point. Value: 7.5. Most recent: 3, 7.5.", description)
    }

    @Test
    fun `describe returns no data message for empty input`() {
        assertEquals("Bar chart. No data.", BarChartAccessibility.describe(emptyList()))
        assertEquals(
            "Step count bar chart. No data.",
            BarChartAccessibility.describe(emptyList(), title = "Step count")
        )
    }

    @Test
    fun `describeBar includes label, value, unit, and position`() {
        val mark = ChartMark(x = 1.0, y = 8432.0, label = "Monday")

        val description = BarChartAccessibility.describeBar(mark, index = 1, total = 7, unit = "steps")

        assertEquals("Monday, 8,432 steps. Bar 2 of 7.", description)
    }

    @Test
    fun `describeBar omits position when there is only one bar`() {
        val mark = ChartMark(x = 0.0, y = 5.0, label = "Only")

        val description = BarChartAccessibility.describeBar(mark, index = 0, total = 1, unit = "kg")

        assertEquals("Only, 5 kg.", description)
    }

    @Test
    fun `describeBar falls back to x value when label is missing`() {
        val mark = ChartMark(x = 2.0, y = 3.25)

        val description = BarChartAccessibility.describeBar(mark, index = 2, total = 4)

        assertEquals("2, 3.3. Bar 3 of 4.", description)
    }

    @Test
    fun `describeRangeChart summarizes title, count, overall range, and most recent point`() {
        val data = listOf(
            RangeChartMark(x = 0.0, minPoint = ChartMark(x = 0.0, y = 58.0), maxPoint = ChartMark(x = 0.0, y = 120.0), label = "Mon"),
            RangeChartMark(x = 1.0, minPoint = ChartMark(x = 1.0, y = 62.0), maxPoint = ChartMark(x = 1.0, y = 162.0), label = "Tue"),
            RangeChartMark(x = 2.0, minPoint = ChartMark(x = 2.0, y = 70.0), maxPoint = ChartMark(x = 2.0, y = 142.0), label = "Wed"),
        )

        val description = BarChartAccessibility.describeRangeChart(data, title = "Heart rate", unit = "bpm")

        assertEquals(
            "Heart rate range bar chart. 3 data points. Range: 58 to 162 bpm. Most recent: Wed, 70 to 142 bpm.",
            description
        )
    }

    @Test
    fun `describeRangeChart omits title prefix when title is null or blank`() {
        val data = listOf(
            RangeChartMark(x = 0.0, minPoint = ChartMark(x = 0.0, y = 60.0), maxPoint = ChartMark(x = 0.0, y = 100.0), label = "Mon")
        )

        assertEquals(
            "Range bar chart. 1 data point. Range: 60 to 100. Most recent: Mon, 60 to 100.",
            BarChartAccessibility.describeRangeChart(data, title = null)
        )
        assertEquals(
            "Range bar chart. 1 data point. Range: 60 to 100. Most recent: Mon, 60 to 100.",
            BarChartAccessibility.describeRangeChart(data, title = "  ")
        )
    }

    @Test
    fun `describeRangeChart uses Value phrasing when min equals max throughout`() {
        val data = listOf(
            RangeChartMark(x = 0.0, minPoint = ChartMark(x = 0.0, y = 50.0), maxPoint = ChartMark(x = 0.0, y = 50.0), label = "Mon")
        )

        val description = BarChartAccessibility.describeRangeChart(data, unit = "kg")

        assertEquals("Range bar chart. 1 data point. Value: 50 kg. Most recent: Mon, 50 kg.", description)
    }

    @Test
    fun `describeRangeChart falls back to x value when label is missing`() {
        val data = listOf(
            RangeChartMark(x = 3.0, minPoint = ChartMark(x = 3.0, y = 40.0), maxPoint = ChartMark(x = 3.0, y = 90.0), label = null)
        )

        val description = BarChartAccessibility.describeRangeChart(data)

        assertEquals("Range bar chart. 1 data point. Range: 40 to 90. Most recent: 3, 40 to 90.", description)
    }

    @Test
    fun `describeRangeChart returns no data message for empty input`() {
        assertEquals("Range bar chart. No data.", BarChartAccessibility.describeRangeChart(emptyList()))
        assertEquals(
            "Heart rate range bar chart. No data.",
            BarChartAccessibility.describeRangeChart(emptyList(), title = "Heart rate")
        )
    }

    @Test
    fun `describeRangeBar includes label, min-to-max range, unit, and position`() {
        val mark = RangeChartMark(x = 1.0, minPoint = ChartMark(x = 1.0, y = 70.0), maxPoint = ChartMark(x = 1.0, y = 156.0), label = "Monday")

        val description = BarChartAccessibility.describeRangeBar(mark, index = 2, total = 7, unit = "bpm")

        assertEquals("Monday, 70 to 156 bpm. Bar 3 of 7.", description)
    }

    @Test
    fun `describeRangeBar uses single value phrasing when min equals max`() {
        val mark = RangeChartMark(x = 0.0, minPoint = ChartMark(x = 0.0, y = 50.0), maxPoint = ChartMark(x = 0.0, y = 50.0), label = "Mon")

        val description = BarChartAccessibility.describeRangeBar(mark, index = 0, total = 5, unit = "kg")

        assertEquals("Mon, 50 kg. Bar 1 of 5.", description)
    }

    @Test
    fun `describeRangeBar omits position when there is only one bar`() {
        val mark = RangeChartMark(x = 0.0, minPoint = ChartMark(x = 0.0, y = 60.0), maxPoint = ChartMark(x = 0.0, y = 100.0), label = "Only")

        val description = BarChartAccessibility.describeRangeBar(mark, index = 0, total = 1, unit = "bpm")

        assertEquals("Only, 60 to 100 bpm.", description)
    }

    @Test
    fun `describeRangeBar falls back to x value when label is missing`() {
        val mark = RangeChartMark(x = 2.0, minPoint = ChartMark(x = 2.0, y = 30.0), maxPoint = ChartMark(x = 2.0, y = 33.25), label = null)

        val description = BarChartAccessibility.describeRangeBar(mark, index = 2, total = 4)

        assertEquals("2, 30 to 33.3. Bar 3 of 4.", description)
    }
}
