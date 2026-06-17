package com.labfreezer.ui.screens.layers

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DeleteConfirmDialog(
    title: String = "\u786e\u8ba4\u5220\u9664",
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("\u5220\u9664") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") }
        }
    )
}
