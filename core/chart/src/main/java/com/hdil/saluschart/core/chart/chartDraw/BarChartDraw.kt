package com.hdil.saluschart.core.chart.chartDraw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hdil.saluschart.core.chart.BaseChartMark
import com.hdil.saluschart.core.chart.ChartMark
import com.hdil.saluschart.core.chart.ChartType
import com.hdil.saluschart.core.chart.RangeChartMark
import com.hdil.saluschart.core.chart.StackedChartMark
import com.hdil.saluschart.core.chart.accessibility.BarChartAccessibility
import com.hdil.saluschart.core.chart.chartMath.ChartMath
import com.hdil.saluschart.core.chart.model.BarCornerRadiusFractions

/**
 * Tooltip payload emitted by [BarChartDraw.BarMarker] via [onTooltipSpec].
 *
 * @property chartMark The mark associated with the currently “active” bar (selected or externally targeted).
 * @property offset Anchor position in *canvas coordinates* (pixels) to position a tooltip near the bar.
 */
data class TooltipSpec(
    val chartMark: BaseChartMark,
    val offset: Offset
)

/**
 * Drawing/marker utilities for bar-based charts (BAR / RANGE_BAR / STACKED_BAR).
 *
 * This object intentionally contains rendering helpers used by multiple chart composables.
 * Public APIs should remain stable to support library consumers.
 */
object BarChartDraw {
    /**
     * Draws X-axis tick labels for a bar chart.
     *
     * Notes:
     * - Labels are anchored using the bar-slot spacing, so the first label starts at
     *   `paddingX + (barWidth / 2)` and proceeds by slot spacing.
     * - If [xLabelAutoSkip] is enabled, labels may be skipped to avoid overlapping.
     *
     * @param ctx Draw context whose native canvas is used to render text.
     * @param labels Full list of labels (one per bar slot).
     * @param metrics Chart layout metrics used for positioning.
     * @param centered Whether text is center-aligned (default: true).
     * @param textSize Text size in pixels (default: 28f).
     * @param maxXTicksLimit Optional cap on how many labels can be displayed (null = no cap).
     * @param xLabelAutoSkip If true, compute an auto-skip factor based on text width and chart width.
     */
    fun drawBarXAxisLabels(
        ctx: DrawContext,
        labels: List<String>,
        metrics: ChartMath.ChartMetrics,
        centered: Boolean = true,
        textSize: Float = 28f,
        maxXTicksLimit: Int? = null,
        xLabelAutoSkip: Boolean = false,
        labelYOffset: Float = 50f
    ) {
        val (displayLabels, displayIndices) = if (xLabelAutoSkip) {
            ChartMath.computeAutoSkipLabels(
                labels = labels,
                textSize = textSize,
                chartWidth = metrics.chartWidth,
                maxXTicksLimit = maxXTicksLimit
            )
        } else {
            Pair(labels, labels.indices.toList())
        }
        
        val totalLabels = labels.size
        val barWidth = metrics.chartWidth / totalLabels / 2
        val spacing = metrics.chartWidth / totalLabels

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            this.textSize = textSize
            textAlign = if (centered) android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT
        }

        val labelY = metrics.paddingY + metrics.chartHeight + labelYOffset

        displayLabels.forEachIndexed { displayIndex, label ->
            val originalIndex = displayIndices[displayIndex]
            val x = metrics.paddingX + barWidth + originalIndex * spacing

            // Clamp only the left edge so the first label never overflows into the y-axis area.
            // The right edge is intentionally left unclamped: clamping the last label shifts it
            // away from its bar centre, making it appear misaligned with the other labels.
            val labelHalfWidth = if (centered) paint.measureText(label) / 2f else 0f
            val clampedX = x.coerceAtLeast(metrics.paddingX + labelHalfWidth)

            ctx.canvas.nativeCanvas.drawText(label, clampedX, labelY, paint)
        }
    }

    /**
     * Renders bar markers as composables (rectangles) positioned using [metrics].
     *
     * Supports:
     * - Regular bars (min = chart min, max = value)
     * - Range bars (min..max)
     * - Stacked bars (tooltip anchor uses total height)
     * - Optional “touch strips” via [isTouchArea]
     *
     * Tooltip behavior:
     * - When [showTooltipForIndex] is provided, that index is treated as externally targeted.
     * - Otherwise, when [interactive] is enabled, tapping a bar toggles internal selection.
     * - Tooltip emission is done through [onTooltipSpec] (anchor in canvas px).
     *
     * Corner radius:
     * - Use [barCornerRadiusFraction] for a uniform fraction-of-width rounding.
     * - Use [barCornerRadiusFractions] for per-corner control.
     * - If [roundTopOnly] is true (default), bottom corners are not rounded unless explicitly provided.
     *
     * @param data Marks (used for tooltip payload).
     * @param minValues Per-bar min values (0 for standard bars, or actual min for range bars).
     * @param maxValues Per-bar max values.
     * @param metrics Chart metrics for layout calculations.
     * @param color Base bar color.
     * @param barWidthRatio Bar width relative to its slot (default: 0.8f).
     * @param interactive If true, taps update internal selection; if false, bars are still drawn as usual.
     * @param useLineChartPositioning If true, use the line-chart spacing rule to position bars.
     * @param onBarClick Callback invoked on bar click: (index, tooltipText).
     * @param chartType Chart type (affects tooltip anchoring).
     * @param showTooltipForIndex Externally-controlled tooltip index; null disables external targeting.
     * @param isTouchArea If true, draws transparent full-height bars for hit testing.
     * @param customTooltipText Optional per-index tooltip text override.
     * @param segmentIndex Segment index in stacked bars (currently used only as a parameter placeholder).
     * @param showLabel Whether to draw a simple value label inside the bar.
     * @param unit Unit suffix. Used in each bar's TalkBack description when [chartType] is
     *   [ChartType.BAR] (see [BarChartAccessibility.describeBar]); unused otherwise.
     * @param barCornerRadiusFraction Uniform corner rounding as a fraction of bar width.
     * @param barCornerRadiusFractions Per-corner rounding fractions of bar width.
     * @param roundTopOnly If true, rounds top corners only when using [barCornerRadiusFraction].
     * @param onTooltipSpec Emits the computed [TooltipSpec] for the active bar, or null if none.
     * @param showBarValueLabels Currently unused; kept for API stability.
     * @param barValueLabel Currently unused; kept for API stability.
     */
    @Composable
    fun BarMarker(
        data: List<BaseChartMark>,
        minValues: List<Double>,
        maxValues: List<Double>,
        metrics: ChartMath.ChartMetrics,
        color: Color = Color.Black,
        barWidthRatio: Float = 0.8f,
        interactive: Boolean = true,
        useLineChartPositioning: Boolean = false,
        onBarClick: ((Int, String) -> Unit)? = null,
        chartType: ChartType,
        showTooltipForIndex: Int? = null,
        isTouchArea: Boolean = false,
        customTooltipText: List<String>? = null,
        segmentIndex: Int? = null,
        showLabel: Boolean = false,
        unit: String = "",
        barCornerRadiusFraction: Float = 0f,
        barCornerRadiusFractions: BarCornerRadiusFractions? = null,
        roundTopOnly: Boolean = true,
        onTooltipSpec: ((TooltipSpec?) -> Unit)? = null,
        showBarValueLabels: Boolean = false,
        barValueLabel: ((index: Int, mark: RangeChartMark) -> String)? = null
    ) {
        val density = LocalDensity.current

        // Kept as state to preserve the “emit on commit” behavior via SideEffect.
        var tooltipSpec by remember { mutableStateOf<TooltipSpec?>(null) }
        var computedTooltip: TooltipSpec? = null

        // Touch-strip mode forces full-width hit area and always allows tapping.
        val actualBarWidthRatio = if (isTouchArea) 1.0f else barWidthRatio
        val actualInteractive = if (isTouchArea) true else interactive

        val dataSize = minOf(data.size, minValues.size, maxValues.size)
        if (dataSize <= 0) return

        // Tracks internal selection (used only when showTooltipForIndex == null and interactive enabled).
        var clickedBarIndex by remember { mutableStateOf<Int?>(null) }

        (0 until dataSize).forEach { index ->
            val minValue = minValues.getOrNull(index) ?: 0.0
            val maxValue = maxValues.getOrNull(index) ?: 0.0

            val tooltipText = customTooltipText?.getOrNull(index) ?: run {
                if (minValue == metrics.minY) {
                    maxValue.toInt().toString()
                } else {
                    "${minValue.toInt()}-${maxValue.toInt()}"
                }
            }

            // Compute bar height and top Y in canvas coordinates.
            val (barHeight, barY) = if (isTouchArea) {
                Pair(metrics.chartHeight, metrics.paddingY)
            } else {
                val yMinScreen = metrics.chartHeight - ((minValue - metrics.minY) / (metrics.maxY - metrics.minY)) * metrics.chartHeight
                val yMaxScreen = metrics.chartHeight - ((maxValue - metrics.minY) / (metrics.maxY - metrics.minY)) * metrics.chartHeight
                val height = yMinScreen - yMaxScreen
                Pair(height, metrics.paddingY + yMaxScreen)
            }

            // X positioning: either line-chart spacing rule or bar-slot centering rule.
            val (barWidth, barX) = if (useLineChartPositioning) {
                val total = dataSize
                val spacing = if (total > 0) metrics.chartWidth / total else 0f
                val pointX = metrics.paddingX + (index + 0.5) * spacing
                val barW = spacing * actualBarWidthRatio
                val barXPos = pointX - barW / 2f
                Pair(barW, barXPos)
            } else {
                val barW = metrics.chartWidth / dataSize * actualBarWidthRatio
                val spacing = metrics.chartWidth / dataSize
                val barXPos = metrics.paddingX + index * spacing + (spacing - barW) / 2f
                Pair(barW, barXPos)
            }

            // Convert px -> dp for layout modifiers.
            val barXDp = with(density) { barX.toFloat().toDp() }
            val barYDp = with(density) { barY.toFloat().toDp() }
            val barWidthDp = with(density) { barWidth.toFloat().toDp() }
            val barHeightDp = with(density) { barHeight.toFloat().toDp() }

            // Decide whether this bar should produce a tooltip anchor.
            val shouldShowTooltip = when {
                isTouchArea -> false
                chartType in listOf(ChartType.BAR, ChartType.RANGE_BAR, ChartType.STACKED_BAR) -> {
                    if (showTooltipForIndex != null) {
                        showTooltipForIndex == index
                    } else if (actualInteractive) {
                        clickedBarIndex == index
                    } else {
                        false
                    }
                }
                else -> false
            }

            if (shouldShowTooltip) {
                val anchorY = if (chartType == ChartType.STACKED_BAR) {
                    val totalValue = maxValues.getOrNull(index) ?: 0.0
                    val denom = (metrics.maxY - metrics.minY).takeIf { it != 0.0 } ?: 1.0
                    val yTopScreen =
                        metrics.chartHeight - ((totalValue - metrics.minY) / denom) * metrics.chartHeight
                    (metrics.paddingY + yTopScreen).toFloat()
                } else {
                    barY.toFloat()
                }

                val mark = data.getOrNull(index) ?: return@forEach
                computedTooltip = TooltipSpec(
                    chartMark = mark,
                    offset = Offset(
                        x = barX.toFloat() + barWidth / 2f,
                        y = anchorY
                    )
                )
            }

            val actualColor = if (isTouchArea) {
                Color.Transparent
            } else {
                if (actualInteractive) {
                    if (clickedBarIndex == index || clickedBarIndex == null) {
                        color
                    } else {
                        color.copy(alpha = 0.3f)
                    }
                } else {
                    if (showTooltipForIndex == index || showTooltipForIndex == null) {
                        color
                    } else {
                        if (color.alpha == 0f) Color.Transparent else color.copy(alpha = 0.3f)
                    }
                }
            }

            val barWidthPx = barWidth
            val barHeightPx = barHeight.toFloat()

            fun fractionToCornerDp(fraction: Float): Dp {
                if (isTouchArea) return 0.dp
                if (fraction <= 0f) return 0.dp
                if (barHeightPx <= 0f || barWidthPx <= 0f) return 0.dp

                val radiusPx = (barWidthPx * fraction)
                    .coerceAtMost(barWidthPx / 2f)
                    .coerceAtMost(barHeightPx / 2f)

                return with(density) { radiusPx.toDp() }
            }

            val fractions = barCornerRadiusFractions ?: run {
                if (roundTopOnly) {
                    BarCornerRadiusFractions(
                        topStart = barCornerRadiusFraction,
                        topEnd = barCornerRadiusFraction,
                        bottomStart = 0f,
                        bottomEnd = 0f
                    )
                } else {
                    BarCornerRadiusFractions(
                        topStart = barCornerRadiusFraction,
                        topEnd = barCornerRadiusFraction,
                        bottomStart = barCornerRadiusFraction,
                        bottomEnd = barCornerRadiusFraction
                    )
                }
            }

            val topStartDp = fractionToCornerDp(fractions.topStart)
            val topEndDp = fractionToCornerDp(fractions.topEnd)
            val bottomStartDp = fractionToCornerDp(fractions.bottomStart)
            val bottomEndDp = fractionToCornerDp(fractions.bottomEnd)

            val hasAnyCorner =
                topStartDp > 0.dp || topEndDp > 0.dp || bottomStartDp > 0.dp || bottomEndDp > 0.dp

            val shape = if (!isTouchArea && hasAnyCorner) {
                RoundedCornerShape(
                    topStart = topStartDp,
                    topEnd = topEndDp,
                    bottomStart = bottomStartDp,
                    bottomEnd = bottomEndDp
                )
            } else null

            var barModifier = Modifier
                .offset(x = barXDp, y = barYDp)
                .size(width = barWidthDp, height = barHeightDp)

            barModifier =
                if (shape != null) {
                    barModifier
                        .clip(shape)
                        .background(color = actualColor, shape = shape)
                } else {
                    barModifier.background(color = actualColor)
                }

            // Keep behavior: clickable is enabled whenever callback is provided.
            barModifier = barModifier.clickable(enabled = (onBarClick != null)) {
                if (actualInteractive) {
                    clickedBarIndex = if (clickedBarIndex == index) null else index
                }
                onBarClick?.invoke(index, tooltipText)
            }

            // Per-bar TalkBack description. All three bar-family chart types are handled below.
            // StackedBarChart gets one composite description per whole bar, not per segment:
            // `data[index]` is always the full StackedChartMark regardless of which of the
            // (up to 4) BarMarker calls this is, since only the whole-bar touch overlay is ever
            // actually interactive — the per-segment color bands (drawn in a separate, earlier
            // BarMarker call per segment layer) never have an onBarClick and so are correctly
            // excluded by the `isReachableLayer` gate below.
            //
            // Gated on `onBarClick != null`, not `actualInteractive`: that flag is only a
            // reliable signal for "is this the Box TalkBack should actually reach" by coincidence
            // for BarChart. InteractionType.Bar.TOUCH_AREA (and RangeBar's equivalent) render two
            // Box layers per bar — a non-interactive visual layer, then an invisible full-height
            // touch strip on top that actually receives taps/TalkBack focus — and
            // `actualInteractive` happens to track that split correctly there. But
            // RangeBarChart's own single-layer `InteractionType.RangeBar.BAR` mode hardcodes
            // `interactive = false` even though it has a real `onBarClick` and is genuinely
            // clickable (a pre-existing quirk in RangeBarChart, not something this change fixes).
            // `onBarClick != null` is the actual ground truth — it's the same condition already
            // driving this Box's own `.clickable(enabled = (onBarClick != null))` a few lines up —
            // and is correct in every mode of both chart types.
            val isReachableLayer = onBarClick != null
            when (chartType) {
                ChartType.BAR -> if (isReachableLayer) {
                    (data.getOrNull(index) as? ChartMark)?.let { mark ->
                        val description = BarChartAccessibility.describeBar(mark, index, dataSize, unit)
                        barModifier = barModifier.semantics { this.contentDescription = description }
                    }
                }
                ChartType.RANGE_BAR -> if (isReachableLayer) {
                    (data.getOrNull(index) as? RangeChartMark)?.let { mark ->
                        val description = BarChartAccessibility.describeRangeBar(mark, index, dataSize, unit)
                        barModifier = barModifier.semantics { this.contentDescription = description }
                    }
                }
                ChartType.STACKED_BAR -> if (isReachableLayer) {
                    (data.getOrNull(index) as? StackedChartMark)?.let { mark ->
                        val description = BarChartAccessibility.describeStackedBar(mark, index, dataSize, unit)
                        barModifier = barModifier.semantics { this.contentDescription = description }
                    }
                }
                else -> Unit
            }

            Box(
                modifier = barModifier,
                contentAlignment = Alignment.TopCenter
            ) {
                if (showLabel) {
                    Box(
                        modifier = Modifier.offset(0.dp, 0.dp) // 바 위에 표시
                    ) {
                        Text(
                            text = maxValue.toInt().toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        tooltipSpec = computedTooltip

        SideEffect {
            onTooltipSpec?.invoke(tooltipSpec)
        }
    }
}