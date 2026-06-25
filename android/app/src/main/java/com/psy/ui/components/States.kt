package com.psy.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.theme.LocalPsyColors

/** Calm empty-state block: Lucide icon tile + title + caption. */
@Composable
fun EmptyState(
    iconName: String,
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(32.dp),
    ) {
        IconTile(iconName, colors.text3, colors.sunken, size = 56.dp)
        Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(caption, color = colors.text3, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/** Shimmer placeholder block for loading lists/cards. */
@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val colors = LocalPsyColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    androidx.compose.foundation.layout.Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.sunken.copy(alpha = alpha)),
    )
}
