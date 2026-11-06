package de.uhrwerksarchiv.watchaccurancy.ui.screens.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.uhrwerksarchiv.watchaccurancy.data.model.RateCalculator
import de.uhrwerksarchiv.watchaccurancy.data.model.RatePoint
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ChartMetric {
    RATE_PER_DAY,
    CUMULATIVE_DEVIATION
}

@Composable
fun WatchPrecisionChart(
    ratePoints: List<RatePoint>,
    metric: ChartMetric,
    modifier: Modifier = Modifier
) {
    if (ratePoints.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Noch keine Messpunkte für dieses Diagramm vorhanden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val displayPoints = remember(ratePoints, metric) {
        when (metric) {
            ChartMetric.RATE_PER_DAY -> ratePoints.filter { it.rateSecondsPerDay != null }
            ChartMetric.CUMULATIVE_DEVIATION -> ratePoints
        }
    }

    if (displayPoints.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mindestens 2 Messpunkte erforderlich für Gangabweichung (s/d).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val primaryLineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val zeroLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .testTag("precision_chart")
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val paddingLeft = 110f
        val paddingRight = 40f
        val paddingTop = 40f
        val paddingBottom = 70f

        val plotWidth = max(10f, width - paddingLeft - paddingRight)
        val plotHeight = max(10f, height - paddingTop - paddingBottom)

        val values = displayPoints.map {
            when (metric) {
                ChartMetric.RATE_PER_DAY -> it.rateSecondsPerDay ?: 0.0
                ChartMetric.CUMULATIVE_DEVIATION -> it.cumulativeDeviationSeconds
            }
        }
        val rawMinY = values.minOrNull() ?: 0.0
        val rawMaxY = values.maxOrNull() ?: 0.0

        val minY = min(rawMinY - 1.0, -1.0)
        val maxY = max(rawMaxY + 1.0, 1.0)
        val yRange = max(0.1, maxY - minY)

        val minX = displayPoints.first().log.ntpTimestampMs
        val maxX = displayPoints.last().log.ntpTimestampMs
        val xRange = max(1000L, maxX - minX).toDouble()

        fun getX(index: Int): Float {
            if (displayPoints.size == 1) return paddingLeft + plotWidth / 2f
            val time = displayPoints[index].log.ntpTimestampMs
            val progress = ((time - minX) / xRange).toFloat()
            return paddingLeft + (progress * plotWidth)
        }

        fun getY(value: Double): Float {
            val progress = ((value - minY) / yRange).toFloat()
            return paddingTop + plotHeight - (progress * plotHeight)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(displayPoints) {
                    detectTapGestures { offset ->
                        var closestIndex: Int? = null
                        var minDistance = Float.MAX_VALUE
                        for (i in displayPoints.indices) {
                            val px = getX(i)
                            val dist = abs(offset.x - px)
                            if (dist < minDistance && dist < 120f) {
                                minDistance = dist
                                closestIndex = i
                            }
                        }
                        selectedPointIndex = closestIndex
                    }
                }
        ) {
            if (minY <= 0.0 && maxY >= 0.0) {
                val zeroY = getY(0.0)
                drawLine(
                    color = zeroLineColor,
                    start = Offset(paddingLeft, zeroY),
                    end = Offset(paddingLeft + plotWidth, zeroY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "0.0",
                    topLeft = Offset(paddingLeft - 85f, zeroY - 18f),
                    style = TextStyle(color = zeroLineColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }

            val stepY = if (yRange > 20) 10.0 else if (yRange > 8) 5.0 else if (yRange > 4) 2.0 else 1.0
            var currentGridY = (minY / stepY).toInt() * stepY
            while (currentGridY <= maxY) {
                if (abs(currentGridY) > 0.01) {
                    val gy = getY(currentGridY)
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, gy),
                        end = Offset(paddingLeft + plotWidth, gy),
                        strokeWidth = 1f
                    )
                    val label = String.format(Locale.US, "%+.1f", currentGridY)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(paddingLeft - 95f, gy - 16f),
                        style = TextStyle(color = textColor, fontSize = 10.sp)
                    )
                }
                currentGridY += stepY
            }

            if (displayPoints.size > 1) {
                val linePath = Path()
                for (i in displayPoints.indices) {
                    val x = getX(i)
                    val y = getY(
                        when (metric) {
                            ChartMetric.RATE_PER_DAY -> displayPoints[i].rateSecondsPerDay ?: 0.0
                            ChartMetric.CUMULATIVE_DEVIATION -> displayPoints[i].cumulativeDeviationSeconds
                        }
                    )
                    if (i == 0) {
                        linePath.moveTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = primaryLineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            for (i in displayPoints.indices) {
                val point = displayPoints[i]
                val x = getX(i)
                val y = getY(
                    when (metric) {
                        ChartMetric.RATE_PER_DAY -> point.rateSecondsPerDay ?: 0.0
                        ChartMetric.CUMULATIVE_DEVIATION -> point.cumulativeDeviationSeconds
                    }
                )

                val posColor = point.log.position.color

                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = posColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )

                if (selectedPointIndex == i) {
                    drawCircle(
                        color = posColor.copy(alpha = 0.35f),
                        radius = 14.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawLine(
                        color = posColor,
                        start = Offset(x, paddingTop),
                        end = Offset(x, paddingTop + plotHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }
            }

            if (displayPoints.isNotEmpty()) {
                val firstDate = RateCalculator.formatDateTime(displayPoints.first().log.ntpTimestampMs)
                drawText(
                    textMeasurer = textMeasurer,
                    text = firstDate,
                    topLeft = Offset(paddingLeft, height - 35f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )

                if (displayPoints.size > 1) {
                    val lastDate = RateCalculator.formatDateTime(displayPoints.last().log.ntpTimestampMs)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = lastDate,
                        topLeft = Offset(paddingLeft + plotWidth - 140f, height - 35f),
                        style = TextStyle(color = textColor, fontSize = 10.sp)
                    )
                }
            }
        }

        selectedPointIndex?.let { idx ->
            if (idx in displayPoints.indices) {
                val point = displayPoints[idx]
                val px = getX(idx)
                val py = getY(
                    when (metric) {
                        ChartMetric.RATE_PER_DAY -> point.rateSecondsPerDay ?: 0.0
                        ChartMetric.CUMULATIVE_DEVIATION -> point.cumulativeDeviationSeconds
                    }
                )

                val tooltipWidth = 180.dp
                val tooltipXOffset = if (px > width / 2) {
                    (px - 190).coerceAtLeast(10f)
                } else {
                    (px + 15).coerceAtMost(width - 190)
                }

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(tooltipXOffset.roundToInt(), (py - 50).coerceAtLeast(10f).roundToInt()) }
                        .width(tooltipWidth),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(point.log.position.color, CircleShape)
                            )
                            Text(
                                text = point.log.position.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = point.log.position.color
                            )
                        }
                        Text(
                            text = RateCalculator.formatDateTime(point.log.ntpTimestampMs),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Gang: ${point.formattedRate}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Abweichung: ${point.formattedDeviation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (point.log.notes.isNotBlank()) {
                            Text(
                                text = "Notiz: ${point.log.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
