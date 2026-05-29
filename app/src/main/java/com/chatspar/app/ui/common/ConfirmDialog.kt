package com.chatspar.app.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "取消",
    neutralText: String? = null,
    onNeutralClick: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    neutralEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = description)
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = onConfirm,
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            Row {
                if (neutralText != null && onNeutralClick != null) {
                    TextButton(
                        enabled = neutralEnabled,
                        onClick = onNeutralClick,
                    ) {
                        Text(text = neutralText)
                    }
                }
                TextButton(
                    enabled = dismissEnabled,
                    onClick = onDismiss,
                ) {
                    Text(text = dismissText)
                }
            }
        },
    )
}
