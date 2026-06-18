package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EMOJI_LIST = listOf(
    "🍜", "🚌", "🛍️", "🧾", "🎮", "💊",
    "📦", "💰", "🎁", "🏠", "🚗", "☕",
    "🍺", "👕", "🏥", "🎬", "📱", "✈️",
    "🎓", "🐶", "💵", "🏦", "💳", "🪙",
    "📈", "🎀", "🧴", "🍔", "🍰", "🚕",
    "⛽", "🏋️", "🎵", "🛒", "☂️", "🎈",
)

private val COLOR_PALETTE: List<Long> = listOf(
    0xFFA18CFF, 0xFF7FD8FF, 0xFFFF8FD6, 0xFFFF5FA2, 0xFF22C55E,
    0xFFFFB86B, 0xFF6BCB77, 0xFF4D96FF, 0xFFFF6B6B, 0xFFB088F9,
)

// 6 columns × 6 rows = 36 emojis, each cell 48dp → grid height = 288dp
private val GRID_HEIGHT = (6 * 48).dp

@Composable
fun EmojiPicker(
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        userScrollEnabled = false,
        modifier = modifier
            .fillMaxWidth()
            .height(GRID_HEIGHT),
    ) {
        items(EMOJI_LIST) { emoji ->
            val isSelected = emoji == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onPick(emoji) }
                    .padding(4.dp),
            ) {
                Text(
                    text = emoji,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selected: Long,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        COLOR_PALETTE.forEach { colorValue ->
            val isSelected = colorValue == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(colorValue))
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                        else Modifier,
                    )
                    .clickable { onPick(colorValue) },
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
