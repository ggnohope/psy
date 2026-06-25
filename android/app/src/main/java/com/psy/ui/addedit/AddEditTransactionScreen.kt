package com.psy.ui.addedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.psy.domain.model.Account
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.icons.LucideIcon
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val colors = LocalPsyColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe the one-shot done event from the ViewModel.
    LaunchedEffect(Unit) {
        viewModel.doneEvent.collect { onDone() }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Photo picker launcher — no runtime permission needed with the Photo Picker API
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onPickPhoto(uri)
    }

    // Amount validation: numeric and > 0. amountText is digits-only (VM strips non-digits).
    val parsedAmount = uiState.amountText.toLongOrNull()
    val amountInvalid = uiState.amountText.isNotEmpty() && (parsedAmount == null || parsedAmount <= 0L)

    Scaffold(containerColor = colors.bg) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── Header: back + title + (optional) delete ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDone) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Quay lại", tint = colors.text)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (uiState.isEdit) "Sửa giao dịch" else "Thêm giao dịch",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                if (uiState.isEdit) {
                    IconButton(onClick = { viewModel.delete() }) {
                        Icon(Lucide.Trash2, contentDescription = "Xoá giao dịch", tint = colors.red)
                    }
                }
            }

            // ── 1. Type segmented control ──
            SegmentedTypeControl(
                selectedType = uiState.type,
                onTypeSelected = viewModel::onTypeChange,
            )

            // ── 2. Amount display + input ──
            AmountSection(
                amountText = uiState.amountText,
                currency = uiState.currency,
                invalid = amountInvalid,
                onAmountChange = viewModel::onAmountChange,
            )

            // ── 3. Category picker (hidden for TRANSFER) ──
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

            // ── 4. Account picker — TRANSFER: from + to; otherwise single ──
            if (uiState.accounts.isNotEmpty()) {
                if (uiState.type == TxType.TRANSFER) {
                    AccountSection(
                        label = "Từ tài khoản",
                        accounts = uiState.accounts,
                        selectedId = uiState.selectedAccountId,
                        onSelect = viewModel::selectAccount,
                    )
                    AccountSection(
                        label = "Đến tài khoản",
                        accounts = uiState.accounts,
                        selectedId = uiState.toAccountId,
                        onSelect = viewModel::onToAccountChange,
                    )
                    val fromEqTo = uiState.selectedAccountId != null &&
                        uiState.toAccountId != null &&
                        uiState.selectedAccountId == uiState.toAccountId
                    if (fromEqTo) {
                        Text(
                            text = "Hai tài khoản phải khác nhau",
                            color = colors.red,
                            fontFamily = PlexMono,
                            fontSize = 12.sp,
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

            // ── 5. Date + time ──
            DateTimeSection(
                dateMillis = uiState.date,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true },
            )

            // ── 6. Note ──
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                placeholder = { Text("Ghi chú", color = colors.text3) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colors.hair,
                    focusedBorderColor = colors.blue,
                    unfocusedContainerColor = colors.surface,
                    focusedContainerColor = colors.surface,
                    unfocusedTextColor = colors.text,
                    focusedTextColor = colors.text,
                ),
            )

            // ── 7. Photo attachment ──
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

            // ── 8. Save ──
            Button(
                onClick = { viewModel.save(System.currentTimeMillis()) },
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.blue,
                    disabledContainerColor = colors.blue.copy(alpha = 0.4f),
                ),
            ) {
                Text(
                    text = "Lưu giao dịch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Date picker dialog ──
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (uiState.date > 0L) uiState.date else System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) viewModel.onDateChange(selectedMillis)
                    showDatePicker = false
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Time picker dialog ──
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
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Huỷ") }
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
// Type segmented control
// ---------------------------------------------------------------------------

@Composable
private fun SegmentedTypeControl(
    selectedType: TxType,
    onTypeSelected: (TxType) -> Unit,
) {
    // Order mirrors the label list below.
    val order = listOf(TxType.INCOME, TxType.EXPENSE, TxType.TRANSFER)
    com.psy.ui.components.SegmentedControl(
        options = listOf("Thu nhập", "Chi tiêu", "Chuyển khoản"),
        selectedIndex = order.indexOf(selectedType).coerceAtLeast(0),
        onSelect = { onTypeSelected(order[it]) },
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Amount display + input
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSection(
    amountText: String,
    currency: Currency,
    invalid: Boolean,
    onAmountChange: (String) -> Unit,
) {
    val colors = LocalPsyColors.current
    val displayAmount: Long = amountText.toLongOrNull() ?: 0L
    var divisor = 1L
    repeat(currency.fractionDigits) { divisor *= 10L }
    val amountMinor = displayAmount * divisor

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = Money.formatMinor(amountMinor, currency.fractionDigits, currency.symbol),
            fontFamily = SpaceGrotesk,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
        )
        Text(
            text = "Nhập số tiền",
            fontFamily = PlexMono,
            fontSize = 12.sp,
            color = colors.text3,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            placeholder = { Text("Số tiền", color = colors.text3, fontFamily = SpaceGrotesk, fontSize = 18.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (invalid) colors.red else colors.hair, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            isError = invalid,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 18.sp,
                color = colors.text,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                unfocusedContainerColor = colors.surface,
                focusedContainerColor = colors.surface,
                errorContainerColor = colors.surface,
            ),
        )
        if (invalid) {
            Text(
                text = "Số tiền phải lớn hơn 0",
                color = colors.red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Category picker: breadcrumb + parent grid + subcategory pills
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
    val colors = LocalPsyColors.current
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val selectedLeaf = leaves.firstOrNull { it.id == selectedLeafId }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header row: mono label + breadcrumb pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "DANH MỤC",
                fontFamily = PlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                color = colors.text3,
            )
            if (selectedGroup != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.blueSoft)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    LucideIcon(
                        name = selectedLeaf?.icon ?: selectedGroup.icon,
                        tint = colors.blue,
                        size = 14.dp,
                    )
                    Text(
                        text = if (selectedLeaf != null) "${selectedGroup.name} › ${selectedLeaf.name}"
                        else selectedGroup.name,
                        color = colors.blue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Picker card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.hair, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Parent grid — manual 4-column grid (no nested scroll inside the page scroll)
            ParentGrid(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onSelectGroup = onSelectGroup,
            )

            // Hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.hair),
            )

            // Subcategory pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leaves.forEach { leaf ->
                    SubcategoryPill(
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
private fun ParentGrid(
    groups: List<CategoryGroup>,
    selectedGroupId: Long?,
    onSelectGroup: (Long) -> Unit,
) {
    val columns = 4
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        groups.chunked(columns).forEach { rowGroups ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowGroups.forEach { group ->
                    ParentTile(
                        group = group,
                        isSelected = group.id == selectedGroupId,
                        onClick = { onSelectGroup(group.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad the last row so tiles keep equal width.
                repeat(columns - rowGroups.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ParentTile(
    group: CategoryGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (isSelected) colors.blueSoft else colors.sunken)
            .then(
                if (isSelected) Modifier.border(1.5.dp, colors.blue, RoundedCornerShape(11.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LucideIcon(
            name = group.icon,
            tint = if (isSelected) colors.blue else colors.text2,
            size = 21.dp,
        )
        Text(
            text = group.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) colors.blue else colors.text2,
            maxLines = 1,
        )
    }
}

@Composable
private fun SubcategoryPill(
    leaf: Category,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalPsyColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) colors.blue else colors.sunken)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LucideIcon(
            name = leaf.icon,
            tint = if (isSelected) Color.White else colors.text2,
            size = 17.dp,
        )
        Text(
            text = leaf.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else colors.text2,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Account chips
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountSection(
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    label: String,
) {
    val colors = LocalPsyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            fontFamily = PlexMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            color = colors.text3,
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
                    modifier = Modifier.weight(1f),
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
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.blueSoft else colors.sunken)
            .then(
                if (isSelected) Modifier.border(1.5.dp, colors.blue, RoundedCornerShape(8.dp))
                else Modifier,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LucideIcon(
            name = account.icon,
            tint = if (isSelected) colors.blue else colors.text2,
            size = 18.dp,
        )
        Text(
            text = account.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) colors.blue else colors.text2,
            maxLines = 1,
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
    val colors = LocalPsyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ĐÍNH KÈM ẢNH",
            fontFamily = PlexMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            color = colors.text3,
        )

        if (photoUri != null) {
            Box(modifier = Modifier.size(96.dp)) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Ảnh đính kèm",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Xoá ảnh",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, colors.hair, RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .clickable(onClick = onAttachClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Đính kèm ảnh", color = colors.text2, fontSize = 14.sp)
            }
        }

        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.redSoft)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = colors.red,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onErrorDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Đóng",
                        tint = colors.red,
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
    val colors = LocalPsyColors.current
    val zone = ZoneId.systemDefault()
    val zoned = if (dateMillis > 0L) Instant.ofEpochMilli(dateMillis).atZone(zone) else null
    val dateLabel = zoned?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "dd/MM/yyyy"
    val timeLabel = zoned?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "THỜI GIAN",
            fontFamily = PlexMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            color = colors.text3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadOnlyTimeField(
                value = dateLabel,
                onClick = onPickDate,
                modifier = Modifier.weight(1f),
            )
            ReadOnlyTimeField(
                value = timeLabel,
                onClick = onPickTime,
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun ReadOnlyTimeField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.hair, RoundedCornerShape(8.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = value,
            fontFamily = PlexMono,
            fontSize = 15.sp,
            color = colors.text,
        )
    }
}
