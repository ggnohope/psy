package com.psy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.psy.data.settings.AccentPalette
import com.psy.data.settings.ThemeMode
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.SpaceGrotesk

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = LocalPsyColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(22.dp),
    ) {
        // ── In-page header ───────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
            ) {
                Icon(
                    Lucide.ArrowLeft,
                    contentDescription = "Quay lại",
                    tint = colors.text,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Giao diện",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = colors.text,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Theme mode card ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.hair, RoundedCornerShape(14.dp)),
        ) {
            ThemeModeRow(
                label = "Theo hệ thống",
                selected = settings.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )
            HorizontalDivider(color = colors.hair, thickness = 1.dp)
            ThemeModeRow(
                label = "Sáng",
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            HorizontalDivider(color = colors.hair, thickness = 1.dp)
            ThemeModeRow(
                label = "Tối",
                selected = settings.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Accent palette ───────────────────────────────────────────
        EyebrowLabel(text = "Màu nhấn")
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AccentPalette.entries.forEach { accent ->
                AccentSwatch(
                    color = accentColor(accent, colors),
                    selected = settings.accent == accent,
                    onClick = { viewModel.setAccent(accent) },
                )
            }
        }
    }
}

private fun accentColor(accent: AccentPalette, colors: com.psy.ui.theme.PsyColors): Color =
    when (accent) {
        AccentPalette.BLUE -> colors.blue
        AccentPalette.AMBER -> colors.amber
        AccentPalette.TEAL -> colors.teal
    }

@Composable
private fun ThemeModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Custom DS radio: ring + filled dot when selected
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) colors.blue else colors.text3,
                    shape = CircleShape,
                ),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colors.blue),
                )
            }
        }
        Text(
            text = label,
            fontSize = 16.sp,
            color = colors.text,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun AccentSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPsyColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .then(
                if (selected) Modifier.border(3.dp, colors.text, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Icon(
                Lucide.Check,
                contentDescription = "Đang chọn",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
