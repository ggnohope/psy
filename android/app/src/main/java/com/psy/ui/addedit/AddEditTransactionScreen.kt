package com.psy.ui.addedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.psy.domain.model.Account
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.theme.CandyGreen
import com.psy.ui.theme.CandyPinkDeep
import com.psy.ui.theme.CandyViolet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe the one-shot done event from the ViewModel.
    // LaunchedEffect with a stable key so it only launches once per screen.
    LaunchedEffect(Unit) {
        viewModel.doneEvent.collect {
            onDone()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Photo picker launcher — no runtime permission needed with the Photo Picker API
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onPickPhoto(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEdit) "Sửa giao dịch" else "Thêm giao dịch",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                actions = {
                    if (uiState.isEdit) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xoá giao dịch",
                                tint = CandyPinkDeep,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ----------------------------------------------------------------
            // 1. Income / Expense segmented toggle
            // ----------------------------------------------------------------
            SegmentedTypeToggle(
                selectedType = uiState.type,
                onTypeSelected = viewModel::onTypeChange,
            )

            // ----------------------------------------------------------------
            // 2. Amount field
            // ----------------------------------------------------------------
            AmountSection(
                amountText = uiState.amountText,
                currency = uiState.currency,
                onAmountChange = viewModel::onAmountChange,
            )

            // ----------------------------------------------------------------
            // 3. Category picker: group tabs → leaf grid (hidden for TRANSFER)
            // ----------------------------------------------------------------
            if (uiState.type != TxType.TRANSFER && uiState.groups.isNotEmpty()) {
                CategorySection(
                    groups = uiState.groups,
                    leaves = uiState.leaves,
                    selectedGroupId = uiState.selectedGroupId,
                    selectedLeafId = uiState.selectedCategoryId,
                    onSelectGroup = viewModel::selectGroup,
                    onSelectLeaf = viewModel::selectCategory,
                )
            }

            // ----------------------------------------------------------------
            // 4. Account chips — for TRANSFER: two selectors; otherwise single
            // ----------------------------------------------------------------
            if (uiState.accounts.isNotEmpty()) {
                if (uiState.type == TxType.TRANSFER) {
                    // "Từ tài khoản" (from)
                    AccountSection(
                        label = "Từ tài khoản",
                        accounts = uiState.accounts,
                        selectedId = uiState.selectedAccountId,
                        onSelect = viewModel::selectAccount,
                    )
                    // "Đến tài khoản" (to)
                    AccountSection(
                        label = "Đến tài khoản",
                        accounts = uiState.accounts,
                        selectedId = uiState.toAccountId,
                        onSelect = viewModel::onToAccountChange,
                    )
                    // Warn when from == to
                    val fromEqTo = uiState.selectedAccountId != null &&
                        uiState.toAccountId != null &&
                        uiState.selectedAccountId == uiState.toAccountId
                    if (fromEqTo) {
                        Text(
                            text = "Hai tài khoản phải khác nhau",
                            color = CandyPinkDeep,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                } else {
                    AccountSection(
                        label = "Tài khoản",
                        accounts = uiState.accounts,
                        selectedId = uiState.selectedAccountId,
                        onSelect = viewModel::selectAccount,
                    )
                }
            }

            // ----------------------------------------------------------------
            // 5. Date + time picker buttons
            // ----------------------------------------------------------------
            DateTimeSection(
                dateMillis = uiState.date,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true },
            )

            // ----------------------------------------------------------------
            // 6. Note field
            // ----------------------------------------------------------------
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3,
            )

            // ----------------------------------------------------------------
            // 7. Photo attachment
            // ----------------------------------------------------------------
            PhotoSection(
                photoUri = uiState.photoUri,
                errorMessage = uiState.photoErrorMessage,
                onAttachClick = {
                    photoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemoveClick = viewModel::onRemovePhoto,
                onErrorDismiss = viewModel::clearPhotoError,
            )

            // ----------------------------------------------------------------
            // 8. Save button
            // ----------------------------------------------------------------
            Button(
                onClick = { viewModel.save(System.currentTimeMillis()) },
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CandyViolet,
                    disabledContainerColor = CandyViolet.copy(alpha = 0.4f),
                ),
            ) {
                Text(
                    text = "Lưu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ----------------------------------------------------------------
    // Date picker dialog
    // ----------------------------------------------------------------
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (uiState.date > 0L) uiState.date else System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        viewModel.onDateChange(selectedMillis)
                    }
                    showDatePicker = false
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Huỷ")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ----------------------------------------------------------------
    // Time picker dialog
    // ----------------------------------------------------------------
    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(
            if (uiState.date > 0L) uiState.date else System.currentTimeMillis(),
        ).atZone(zone)
        val timePickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true,
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChange(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Huỷ")
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Segmented toggle: Income / Expense
// ---------------------------------------------------------------------------

@Composable
private fun SegmentedTypeToggle(
    selectedType: TxType,
    onTypeSelected: (TxType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.Center,
    ) {
        TxType.entries.forEach { type ->
            val isSelected = type == selectedType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (type) {
                        TxType.EXPENSE -> "Chi tiêu"
                        TxType.INCOME -> "Thu nhập"
                        TxType.TRANSFER -> "Chuyển khoản"
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Amount display + input
// ---------------------------------------------------------------------------

@Composable
private fun AmountSection(
    amountText: String,
    currency: com.psy.domain.model.Currency,
    onAmountChange: (String) -> Unit,
) {
    val displayAmount: Long = amountText.toLongOrNull() ?: 0L
    // For display: reconstruct amountMinor from typed whole-unit value.
    var divisor = 1L
    repeat(currency.fractionDigits) { divisor *= 10L }
    val amountMinor = displayAmount * divisor

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = Money.formatMinor(amountMinor, currency.fractionDigits, currency.symbol),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            label = { Text("Số tiền") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
        )
    }
}

// ---------------------------------------------------------------------------
// Category picker: group tab row + leaf grid (4 columns)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    groups: List<CategoryGroup>,
    leaves: List<Category>,
    selectedGroupId: Long?,
    selectedLeafId: Long?,
    onSelectGroup: (Long) -> Unit,
    onSelectLeaf: (Long) -> Unit,
) {
    Column {
        Text(
            text = "Danh mục",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // ── Group tab row (horizontally scrollable chips) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEach { group ->
                GroupTabChip(
                    group = group,
                    isSelected = group.id == selectedGroupId,
                    onClick = { onSelectGroup(group.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Leaf grid for the selected group ──
        if (leaves.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height((((leaves.size + 3) / 4) * 80).dp),
                userScrollEnabled = false,
            ) {
                items(leaves, key = { it.id }) { leaf ->
                    LeafChip(
                        leaf = leaf,
                        isSelected = leaf.id == selectedLeafId,
                        onSelect = { onSelectLeaf(leaf.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupTabChip(
    group: CategoryGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (isSelected) CandyViolet.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) CandyViolet else Color.Transparent,
                shape = RoundedCornerShape(50.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = group.icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = group.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CandyViolet else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun LeafChip(
    leaf: Category,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) CandyViolet.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) CandyViolet else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = leaf.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = leaf.name,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CandyViolet else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Account chips row
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountSection(
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    label: String = "Tài khoản",
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { account ->
                AccountChip(
                    account = account,
                    isSelected = account.id == selectedId,
                    onSelect = { onSelect(account.id) },
                )
            }
        }
    }
}

@Composable
private fun AccountChip(
    account: Account,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (isSelected) CandyGreen.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) CandyGreen else Color.Transparent,
                shape = RoundedCornerShape(50.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = account.icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = account.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CandyGreen else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------
// Photo section
// ---------------------------------------------------------------------------

@Composable
private fun PhotoSection(
    photoUri: String?,
    errorMessage: String?,
    onAttachClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Đính kèm ảnh",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        if (photoUri != null) {
            // Show thumbnail with a remove (×) overlay
            Box(modifier = Modifier.size(96.dp)) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Ảnh đính kèm",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                // Remove button in top-right corner
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xoá ảnh",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onAttachClick,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "📷  Đính kèm ảnh")
            }
        }

        // Transient error banner
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CandyPinkDeep.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CandyPinkDeep,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onErrorDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = CandyPinkDeep,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Date + time section
// ---------------------------------------------------------------------------

@Composable
private fun DateTimeSection(
    dateMillis: Long,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val zoned = if (dateMillis > 0L) Instant.ofEpochMilli(dateMillis).atZone(zone) else null
    val dateLabel = zoned?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        ?: "Chọn ngày"
    val timeLabel = zoned?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"

    Column {
        Text(
            text = "Thời gian",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Date field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(onClick = onPickDate)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = dateLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // Time field
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(onClick = onPickTime)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = timeLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
