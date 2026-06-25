package com.psy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.User
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PsyTypography

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    onManageAccounts: () -> Unit,
    onAppearance: () -> Unit = {},
    onLockSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val colors = LocalPsyColors.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Đăng xuất sẽ sao lưu rồi xoá dữ liệu trên máy này. Tiếp tục?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Huỷ")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
    ) {
        // In-page header: back + title
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                ) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Quay lại", tint = colors.text)
                }
                Spacer(Modifier.width(8.dp))
                Text("Cài đặt", style = PsyTypography.titleLarge, color = colors.text)
            }
            Spacer(Modifier.height(20.dp))
        }

        // Grouped settings card
        item {
            SettingsCard(colors) {
                SettingsRow(
                    label = "Giao diện",
                    icon = Lucide.Palette,
                    tint = colors.blue,
                    tileBg = colors.blueSoft,
                    onClick = onAppearance,
                )
                Hairline(colors.hair)
                SettingsRow(
                    label = "Khoá ứng dụng",
                    icon = Lucide.Lock,
                    tint = colors.teal,
                    tileBg = colors.tealSoft,
                    onClick = onLockSettings,
                )
                Hairline(colors.hair)
                SettingsRow(
                    label = "Quản lý danh mục",
                    icon = Lucide.List,
                    tint = colors.amber,
                    tileBg = colors.amberSoft,
                    onClick = onManageCategories,
                )
                Hairline(colors.hair)
                SettingsRow(
                    label = "Quản lý tài khoản",
                    icon = Lucide.User,
                    tint = colors.green,
                    tileBg = colors.greenSoft,
                    onClick = onManageAccounts,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Logout card
        item {
            SettingsCard(colors) {
                SettingsRow(
                    label = "Đăng xuất",
                    icon = Lucide.LogOut,
                    tint = colors.red,
                    tileBg = colors.redSoft,
                    labelColor = colors.red,
                    showChevron = false,
                    onClick = { showLogoutDialog = true },
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        // Footer
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EyebrowLabel("PSY · v2.0")
            }
        }
    }
}

/** Grouped list card: surface bg, 14 radius, 1px hair border. */
@Composable
private fun SettingsCard(
    colors: com.psy.ui.theme.PsyColors,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.hair, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

/** 1px hairline divider between rows. */
@Composable
private fun Hairline(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/** Settings row: 38dp tinted IconTile + label + optional ChevronRight, whole row clickable. */
@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    tint: Color,
    tileBg: Color,
    onClick: () -> Unit,
    labelColor: Color? = null,
    showChevron: Boolean = true,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tileBg),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = PsyTypography.bodyLarge,
            color = labelColor ?: colors.text,
            modifier = Modifier.weight(1f),
        )
        if (showChevron) {
            Icon(Lucide.ChevronRight, contentDescription = null, tint = colors.text3)
        }
    }
}
