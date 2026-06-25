package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.psy.ui.theme.LocalPsyColors

@Composable
fun BudgetProgress(
    spentMinor: Long,
    limitMinor: Long,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    /** Override the fill color; defaults to red when over budget else green. */
    fillColor: Color? = null,
) {
    val colors = LocalPsyColors.current
    val fraction = if (limitMinor > 0) (spentMinor.toFloat() / limitMinor).coerceIn(0f, 1f) else 0f
    val over = spentMinor > limitMinor && limitMinor > 0
    val resolvedFill = when {
        over -> colors.red
        fillColor != null -> fillColor
        else -> colors.green
    }
    val trackColor = colors.sunken

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(resolvedFill),
        )
    }
}
