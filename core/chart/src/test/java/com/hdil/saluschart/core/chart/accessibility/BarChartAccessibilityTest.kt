package com.hdil.saluschart.core.chart.accessibility

import com.hdil.saluschart.core.chart.ChartMark
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
}
