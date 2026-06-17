package com.labfreezer.ui.screens.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.labfreezer.ui.screens.move.MoveState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveTarget

@Composable
fun DeviceDialog(
    existing: StorageDeviceEntity? = null,
    deviceTypes: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: (deviceTypes.firstOrNull() ?: "FREEZER_M80")) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    val title = if (existing != null) "编辑设备" else "添加设备"
    val confirmLabel = if (existing != null) "保存" else "添加"

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("设备名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("设备类型") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        deviceTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.matchParentSize().clickable { typeExpanded = true }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), type, note.trim().ifEmpty { null }) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun LayerDialog(
    existing: StorageLayerEntity? = null,
    availableDevices: List<StorageDeviceEntity> = emptyList(),
    currentDeviceId: Long = 0,
    navController: NavController? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, deviceId: Long, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var selectedDeviceId by remember { mutableStateOf(currentDeviceId) }
    var showDevicePicker by remember { mutableStateOf(false) }
    val selectedDevice = availableDevices.find { it.id == selectedDeviceId }

    LaunchedEffect(MoveState.resultDeviceId) {
        MoveState.resultDeviceId?.let { id ->
            selectedDeviceId = id
            MoveState.resultDeviceId = null
        }
    }

    val title = if (existing != null) "编辑层" else "添加层"
    val confirmLabel = if (existing != null) "保存" else "添加"

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                if (existing != null && availableDevices.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (navController != null) {
                                    MoveState.selectMode = true
                                    MoveState.moveTarget = MoveTarget.DEVICE
                                    MoveState.resultDeviceId = null
                                    navController.navigate(Screen.MoveBrowser.route)
                                } else {
                                    showDevicePicker = true
                                }
                            }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("所在设备", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(selectedDevice?.name ?: "请选择设备", fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.OpenWith, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("层名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedDeviceId, note.trim().ifEmpty { null }) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            title = { Text("选择设备", fontWeight = FontWeight.SemiBold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(availableDevices, key = { it.id }) { device ->
                        val isSelected = device.id == selectedDeviceId
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedDeviceId = device.id; showDevicePicker = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(22.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(12.dp))
                                Text(device.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDevicePicker = false }) { Text("取消") } }
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String = "确认删除",
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
