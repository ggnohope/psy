package com.psy.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.ChartColumn
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Wallet
import com.psy.ui.theme.LocalPsyColors

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Trang chủ", Lucide.House),
    BottomNavItem(Routes.STATS, "Thống kê", Lucide.ChartColumn),
    BottomNavItem(Routes.CALENDAR, "Lịch", Lucide.Calendar),
    BottomNavItem(Routes.BUDGET, "Ngân sách", Lucide.Wallet),
)

@Composable
fun PsyBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    val colors = LocalPsyColors.current
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        // top hairline
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(1.dp).background(colors.hair),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bottomNavItems.forEach { item ->
                val active = currentRoute == item.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(item.route) },
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) colors.blueSoft else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (active) colors.blue else colors.text3,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) colors.blue else colors.text3,
                    )
                }
            }
        }
    }
}
