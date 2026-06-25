package com.psy.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk

data class PieSlice(val label: String, val amountMinor: Long, val color: Long)

/**
 * Donut chart with an animated sweep. Slice colors come from the caller's fixed
 * palette (kept as-is). The center shows an optional mono eyebrow [centerTitle]
 * above the SpaceGrotesk [centerLabel] total.
 */
@Composable
fun DonutChart(
    slices: List<PieSlice>,
    centerLabel: String,
    modifier: Modifier = Modifier,
    centerTitle: String? = null,
) {
    val colors = LocalPsyColors.current
    val total = slices.sumOf { it.amountMinor }

    // Animate 0 → 1 on first composition or when slices change
    var targetProgress by remember(slices) { mutableStateOf(0f) }
    LaunchedEffect(slices) { targetProgress = 1f }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "donutProgress",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(200.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 36.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            if (total <= 0L) {
                drawArc(
                    color = colors.sunken,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = arcSize,
                    topLeft = topLeft,
                )
            } else {
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = (360f * slice.amountMinor / total) * progress
                    drawArc(
                        color = Color(slice.color),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = arcSize,
                        topLeft = topLeft,
                    )
                    startAngle += sweep
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (centerTitle != null) {
                Text(
                    text = centerTitle.uppercase(),
                    fontFamily = PlexMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp,
                    color = colors.text3,
                )
            }
            Text(
                text = centerLabel,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.text,
            )
        }
    }
}
