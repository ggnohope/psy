package com.psy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    onManageAccounts: () -> Unit,
    onAppearance: () -> Unit = {},
    onLockSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Giao diện") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onAppearance),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Khoá ứng dụng") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onLockSettings),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Quản lý danh mục") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onManageCategories),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Quản lý tài khoản") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onManageAccounts),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Đăng xuất") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { showLogoutDialog = true },
                )
            }
        }
    }
}
