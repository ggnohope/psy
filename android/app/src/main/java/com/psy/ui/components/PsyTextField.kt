package com.psy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psy.ui.theme.LocalPsyColors

/**
 * HostGuardIQ themed text field: `hair` unfocused border, `blue` focused border,
 * surface/transparent container, `text` content, chip-radius (8.dp) corners.
 * Mirrors iOS PsyTextField.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    val colors = LocalPsyColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.blue,
            unfocusedBorderColor = colors.hair,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            cursorColor = colors.blue,
            focusedLabelColor = colors.blue,
            unfocusedLabelColor = colors.text3,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
