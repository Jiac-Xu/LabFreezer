package com.labfreezer.ui.screens.layers
import com.labfreezer.R

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.isHidden
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun LayerDialog(
    existing: StorageLayerEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    val title = if (existing != null) stringResource(R.string.layer_dialog_title_edit) else stringResource(R.string.layer_dialog_title_add)
    val confirmLabel = if (existing != null) stringResource(R.string.btn_save) else stringResource(R.string.btn_add)

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
                    label = { Text(stringResource(R.string.layer_dialog_label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.label_note_optional)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BoxDialogEntryPoint {
    fun layerRepository(): StorageLayerRepository
    fun deviceRepository(): StorageDeviceRepository
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
    var selectedLayerId by remember { mutableStateOf(existing?.layerId ?: currentLayerId) }
    var showLocationPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, BoxDialogEntryPoint::class.java)
    }
    var locationText by remember { mutableStateOf("") }

    LaunchedEffect(selectedLayerId) {
        if (selectedLayerId > 0) {
            val layer = entryPoint.layerRepository().getById(selectedLayerId)
            if (layer != null) {
                val device = entryPoint.deviceRepository().getById(layer.deviceId)
                val devName = if (device == null || device.isHidden()) null else device.name
                val layName = if (layer.isHidden()) null else layer.name
                val parts = listOfNotNull(devName, layName)
                locationText = if (parts.isEmpty()) context.getString(R.string.location_root_level) else parts.joinToString(" > ")
            } else {
                locationText = context.getString(R.string.location_root_level)
            }
        } else {
            locationText = context.getString(R.string.location_root_level)
        }
    }

    LaunchedEffect(MoveState.resultLayerId) {
        MoveState.resultLayerId?.let { id ->
            selectedLayerId = id
            MoveState.resultLayerId = null
        }
    }

    val title = if (existing != null) stringResource(R.string.box_dialog_title_edit) else stringResource(R.string.box_dialog_title_add)
    val confirmLabel = if (existing != null) stringResource(R.string.btn_save) else stringResource(R.string.btn_add)

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
                if (existing != null) {
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
                                Text(stringResource(R.string.layer_dialog_label_location), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    locationText.ifEmpty { stringResource(R.string.layer_dialog_placeholder_select_location) },
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
                    label = { Text(stringResource(R.string.box_dialog_label_name)) },
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
                        label = { Text(stringResource(R.string.box_dialog_label_rows)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = cols,
                        onValueChange = { cols = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.box_dialog_label_cols)) },
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
                    label = { Text(stringResource(R.string.label_note_optional)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
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
                if (selectedDeviceId != null) stringResource(R.string.layer_dialog_label_parent_layer) else stringResource(R.string.layer_dialog_title_select_device),
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
            }) { Text(if (selectedDeviceId != null) stringResource(R.string.btn_back) else stringResource(R.string.btn_cancel)) }
        }
    )
}
