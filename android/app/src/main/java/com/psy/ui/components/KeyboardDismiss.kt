package com.psy.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Dismiss the soft keyboard when tapping outside a focused text field within this subtree.
 *
 * AppRoot already does this for the main composition, but Material3 `ModalBottomSheet`
 * renders in a separate platform window that the root tap handler can't reach — so sheet
 * contents with text fields (budget amount, account/category editors) apply this directly.
 *
 * `detectTapGestures` only fires for taps that children didn't consume, so text fields,
 * buttons and chips keep working normally.
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return this.pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}
