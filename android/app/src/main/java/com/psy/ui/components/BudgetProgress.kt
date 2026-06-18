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
import androidx.compose.ui.unit.dp
import com.psy.ui.theme.CandyGreen
import com.psy.ui.theme.CandyPinkDeep
import com.psy.ui.theme.CandyViolet

@Composable
fun BudgetProgress(
    spentMinor: Long,
    limitMinor: Long,
    modifier: Modifier = Modifier,
) {
    val fraction = if (limitMinor > 0) (spentMinor.toFloat() / limitMinor).coerceIn(0f, 1f) else 0f
    val fillColor = if (spentMinor > limitMinor && limitMinor > 0) CandyPinkDeep else CandyGreen
    val trackColor = CandyViolet.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fillColor),
        )
    }
}
