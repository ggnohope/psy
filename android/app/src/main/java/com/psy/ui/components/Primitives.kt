package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.icons.LucideIcon
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono

/** Tinted rounded-square tile holding a Lucide icon. */
@Composable
fun IconTile(
    iconName: String,
    tint: Color,
    bg: Color,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.25f)).background(bg),
    ) {
        LucideIcon(iconName, tint = tint, size = size * 0.5f)
    }
}

/** IBM Plex Mono uppercase eyebrow label. */
@Composable
fun EyebrowLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val c = color ?: LocalPsyColors.current.text3
    Text(
        text.uppercase(),
        fontFamily = PlexMono,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp,
        color = c,
        modifier = modifier,
    )
}

/** Single-select segmented control. Active segment = primary bg + white text. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.sunken)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.blue else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
            ) {
                Text(
                    label,
                    color = if (active) Color.White else colors.text2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
