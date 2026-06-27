package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.SpaceGrotesk

/** Navy-gradient hero card with a 3px accent bar across the top edge. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalPsyColors.current
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.heroGradient),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.accentLine)
                .align(Alignment.TopCenter),
        )
        Column(Modifier.padding(22.dp).padding(top = 3.dp), content = content)
    }
}

/** Shared transaction row used on Home and Calendar. */
@Composable
fun TransactionRow(
    iconName: String,
    iconTint: Color,
    iconBg: Color,
    name: String,
    meta: String,
    amount: String,
    isIncome: Boolean,
    account: String,
    isFund: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.hair, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        IconTile(iconName, iconTint, iconBg, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(name, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(meta, color = colors.text3, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                amount,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isIncome) colors.green else colors.red,
            )
            Text(account, color = colors.text3, fontSize = 11.sp)
            if (isFund) {
                Text(
                    "Quỹ",
                    color = colors.blue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colors.blueSoft)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}
