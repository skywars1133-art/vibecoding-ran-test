package com.chesslock.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular countdown for the home hero card: a track plus an animated arc of the remaining share of
 * today's budget, with the clock in the middle. Gauge ticks sit on top of the arc so the ring still
 * reads as a gauge even when the whole circle is filled.
 */
@Composable
fun BudgetRing(
    progress: Float,
    contentColor: Color,
    accentColor: Color,
    timeText: String,
    modifier: Modifier = Modifier,
) {
    val targetProgress = progress.coerceIn(0f, 1f)
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(targetProgress) {
        animatedProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2f
            val diameter = size.minDimension
            val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
            drawArc(
                color = contentColor.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            if (animatedProgress.value > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Gauge ticks: twelve marks spaced around the stroke band, drawn over the arc so the
            // dial stays legible when the budget is full and the arc becomes a complete circle.
            val tickRadius = (diameter - strokeWidth) / 2f
            val tickHalfLength = strokeWidth * 0.28f
            val tickStroke = 2.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(12) { index ->
                val angle = Math.toRadians((-90f + index * 30f).toDouble())
                val cosAngle = cos(angle).toFloat()
                val sinAngle = sin(angle).toFloat()
                val inner = Offset(
                    x = center.x + (tickRadius - tickHalfLength) * cosAngle,
                    y = center.y + (tickRadius - tickHalfLength) * sinAngle,
                )
                val outer = Offset(
                    x = center.x + (tickRadius + tickHalfLength) * cosAngle,
                    y = center.y + (tickRadius + tickHalfLength) * sinAngle,
                )
                drawLine(
                    color = contentColor.copy(alpha = 0.22f),
                    start = inner,
                    end = outer,
                    strokeWidth = tickStroke,
                    cap = StrokeCap.Round,
                )
            }
        }
        Text(
            text = timeText,
            color = contentColor,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Placeholder size used by the hero card; kept here so the ring and its slot stay in sync. */
val BudgetRingSize = 148.dp
