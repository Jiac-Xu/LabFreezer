package com.labfreezer.ui.screens.layers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget

@Composable
fun LayerDialog(
    existing: StorageLayerEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

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
                onClick = { onConfirm(name.trim(), note.trim().ifEmpty { null }) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun BoxDialog(
    existing: StorageBoxEntity? = null,
    availableDevices: List<StorageDeviceEntity> = emptyList(),
    layersByDevice: Map<Long, List<StorageLayerEntity>> = emptyMap(),
    currentLayerId: Long = 0,
    navController: NavController? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, layerId: Long, rows: Int, cols: Int, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var rows by remember { mutableStateOf(existing?.rows?.toString() ?: "9") }
    var cols by remember { mutableStateOf(existing?.cols?.toString() ?: "9") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var selectedLayerId by remember { mutableStateOf(currentLayerId) }
    var showLocationPicker by remember { mutableStateOf(false) }

    LaunchedEffect(MoveState.resultLayerId) {
        MoveState.resultLayerId?.let { id ->
            selectedLayerId = id
            MoveState.resultLayerId = null
        }
    }

    val selectedLayer = layersByDevice.values.flatten().find { it.id == selectedLayerId }
    val selectedDevice = availableDevices.find { d -> layersByDevice[d.id]?.any { it.id == selectedLayerId } == true }

    val title = if (existing != null) "编辑冻存盒" else "添加冻存盒"
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
                                    MoveState.moveTarget = MoveTarget.LAYER
                                    MoveState.resultLayerId = null
                                    navController.navigate(Screen.MoveBrowser.route)
                                } else {
                                    showLocationPicker = true
                                }
                            }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("所在位置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    if (selectedDevice != null && selectedLayer != null) "${selectedDevice.name} > ${selectedLayer.name}"
                                    else "请选择位置",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(Icons.Default.OpenWith, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("冻存盒名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = rows,
                        onValueChange = { rows = it.filter { c -> c.isDigit() } },
                        label = { Text("行数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = cols,
                        onValueChange = { cols = it.filter { c -> c.isDigit() } },
                        label = { Text("列数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = fieldShape,
                        colors = fieldColors
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
                onClick = {
                    val r = rows.toIntOrNull() ?: return@TextButton
                    val c = cols.toIntOrNull() ?: return@TextButton
                    onConfirm(name.trim(), selectedLayerId, r, c, note.trim().ifEmpty { null })
                },
                enabled = name.isNotBlank() && rows.toIntOrNull() != null && cols.toIntOrNull() != null
            ) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showLocationPicker) {
        LocationPickerDialog(
            devices = availableDevices,
            layersByDevice = layersByDevice,
            currentLayerId = selectedLayerId,
            onSelected = { deviceId, layerId ->
                selectedLayerId = layerId
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
    }
}

@Composable
private fun LocationPickerDialog(
    devices: List<StorageDeviceEntity>,
    layersByDevice: Map<Long, List<StorageLayerEntity>>,
    currentLayerId: Long,
    onSelected: (deviceId: Long, layerId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDeviceId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (selectedDeviceId != null) "选择层" else "选择设备",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (selectedDeviceId == null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(devices, key = { it.id }) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedDeviceId = device.id },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(device.name, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                val layers = layersByDevice[selectedDeviceId] ?: emptyList()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(layers, key = { it.id }) { layer ->
                        val isCurrent = layer.id == currentLayerId
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelected(selectedDeviceId!!, layer.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(22.dp), tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(12.dp))
                                Text(layer.name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedDeviceId != null) selectedDeviceId = null
                else onDismiss()
            }) { Text(if (selectedDeviceId != null) "返回" else "取消") }
        }
    )
}
