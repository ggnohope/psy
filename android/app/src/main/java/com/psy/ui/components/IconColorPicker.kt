package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.psy.ui.icons.LucideIcon
import com.psy.ui.icons.LucideIcons
import com.psy.ui.theme.LocalPsyColors

private val COLOR_PALETTE: List<Long> = listOf(
    0xFF0A7CF6, 0xFFF59E0B, 0xFF0BB3B0, 0xFF1F9D62, 0xFFE0413A,
    0xFF3D97F8, 0xFFFBB43D, 0xFF19E3E0, 0xFF5B6B80, 0xFF0A2540,
)

/**
 * Searchable Lucide icon picker (replaces the old fixed-emoji grid).
 * `selected`/`onPick` are Lucide name strings (e.g. "shopping-bag").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    var query by remember { mutableStateOf("") }
    val items = remember(query) {
        if (query.isBlank()) LucideIcons.pickerSet
        else LucideIcons.pickerSet.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Tìm biểu tượng") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEach { name ->
                val isSelected = name == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.blueSoft else colors.sunken)
                        .clickable { onPick(name) },
                ) {
                    LucideIcon(name, tint = if (isSelected) colors.blue else colors.text2, size = 22.dp)
                }
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
