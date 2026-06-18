package com.psy.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.psy.ui.theme.CandyGreen
import com.psy.ui.theme.CandyPinkDeep

data class MonthBars(val label: String, val incomeMinor: Long, val expenseMinor: Long)

@Composable
fun TrendBars(
    months: List<MonthBars>,
    modifier: Modifier = Modifier,
) {
    if (months.isEmpty()) return

    val maxVal = maxOf(1L, months.maxOf { maxOf(it.incomeMinor, it.expenseMinor) })

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            LegendDot(color = CandyGreen)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Thu", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(16.dp))
            LegendDot(color = CandyPinkDeep)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Chi", style = MaterialTheme.typography.labelSmall)
        }

        // Bar chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val slotWidth = canvasWidth / months.size
            val barGap = 4.dp.toPx()           // gap between income & expense bars
            val slotPadding = 6.dp.toPx()      // gap between slots
            val barWidth = (slotWidth - slotPadding * 2 - barGap) / 2f
            val cornerRadius = CornerRadius(6.dp.toPx())

            months.forEachIndexed { index, monthBar ->
                val slotLeft = index * slotWidth + slotPadding
                val incomeHeight = (monthBar.incomeMinor / maxVal.toFloat()) * canvasHeight
                val expenseHeight = (monthBar.expenseMinor / maxVal.toFloat()) * canvasHeight

                // Income bar (left, CandyGreen)
                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = CandyGreen,
                        topLeft = Offset(slotLeft, canvasHeight - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = cornerRadius,
                    )
                }

                // Expense bar (right, CandyPinkDeep)
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = CandyPinkDeep,
                        topLeft = Offset(slotLeft + barWidth + barGap, canvasHeight - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }

        // Month labels row
        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { month ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = month.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .drawBehind { drawCircle(color) },
    )
}
