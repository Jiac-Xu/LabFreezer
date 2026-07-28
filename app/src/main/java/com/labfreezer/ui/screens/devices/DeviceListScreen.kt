package com.labfreezer.ui.screens.devices
import com.labfreezer.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.labfreezer.ui.screens.devices.DeleteConfirmDialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.repository.RecentBox
import com.labfreezer.data.search.ScopeType
import com.labfreezer.ui.components.SpeedDialFAB
import com.labfreezer.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeviceListScreen(
    navController: NavController,
    showFabPadding: Boolean = true,
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val deviceTypeNames by viewModel.deviceTypeNames.collectAsStateWithLifecycle()
    val recentBoxes by viewModel.recentBoxes.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectingRecent by viewModel.isSelectingRecent.collectAsStateWithLifecycle()
    val selectedRecentIds by viewModel.selectedRecentIds.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingDevice by viewModel.editingDevice.collectAsStateWithLifecycle()
    val deletingDevice by viewModel.deletingDevice.collectAsStateWithLifecycle()
    var showDeleteBatchConfirm by remember { mutableStateOf(false) }
    var expandedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var speedDialExpanded by remember { mutableStateOf(false) }
    var showCreateBoxDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshRecentBoxes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val inAnySelection = isSelecting || isSelectingRecent
    val selectionLabel = when {
        isSelecting -> stringResource(R.string.device_list_selected_count, selectedIds.size)
        isSelectingRecent -> stringResource(R.string.device_list_selected_count, selectedRecentIds.size)
        else -> stringResource(R.string.app_name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(selectionLabel, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    if (inAnySelection) {
                        IconButton(onClick = {
                            if (isSelectingRecent) viewModel.exitRecentSelecting()
                            else viewModel.exitSelection()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                        }
                    }
                },
                actions = {
                    if (inAnySelection) {
                        TextButton(onClick = {
                            if (isSelectingRecent) viewModel.selectAllRecent()
                            else viewModel.selectAll()
                        }) {
                            Text(
                                if ((isSelectingRecent && selectedRecentIds.size == recentBoxes.size) ||
                                    (isSelecting && selectedIds.size == devices.size)) stringResource(R.string.device_list_deselect_all) else stringResource(R.string.device_list_select_all),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = {
                            if (isSelectingRecent) viewModel.deleteSelectedRecent()
                            else showDeleteBatchConfirm = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(Screen.Search.createRoute(ScopeType.ALL)) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.device_list_search))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (!inAnySelection) {
                SpeedDialFAB(
                    expanded = speedDialExpanded,
                    onToggle = { speedDialExpanded = !speedDialExpanded },
                    onCreateBox = { showCreateBoxDialog = true; speedDialExpanded = false },
                    onCreateSecond = { viewModel.showAddDialog() },
                    showSecondButton = true,
                    secondButtonLabel = stringResource(R.string.device_list_add_device),
                    secondButtonIcon = Icons.Default.DeviceHub,
                    modifier = if (showFabPadding) Modifier.padding(bottom = 88.dp) else Modifier
                )
            }
        }
    ) { padding ->
        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.DevicesOther, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.device_list_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.device_list_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        } else {
            val groupedTypes = devices.groupBy { it.type }.keys
            LaunchedEffect(devices) {
                if (devices.isNotEmpty()) {
                    val prefs = context.getSharedPreferences("device_group_prefs", android.content.Context.MODE_PRIVATE)
                    val saved = prefs.getStringSet("expanded_types", emptySet()) ?: emptySet()
                    expandedTypes = if (saved.isEmpty()) groupedTypes else saved
                }
            }
            LaunchedEffect(expandedTypes) {
                if (expandedTypes.isNotEmpty()) {
                    val prefs = context.getSharedPreferences("device_group_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putStringSet("expanded_types", expandedTypes).apply()
                }
            }
            var groupByType by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                groupByType = context.getSharedPreferences("device_group_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("group_by_type_enabled", true)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 100.dp)
            ) {
                if (recentBoxes.isNotEmpty()) {
                    item {
                        RecentlyViewedSection(
                            recentBoxes = recentBoxes,
                            isSelecting = isSelectingRecent,
                            selectedIds = selectedRecentIds,
                            onToggleSelect = { viewModel.toggleRecentSelection(it) },
                            onStartSelect = { viewModel.startRecentSelecting(it) },
                            onClick = { navController.navigate(Screen.BoxGrid.createRoute(it)) }
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.device_list_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                }
                if (groupByType) {
                    val grouped = devices.groupBy { it.type }
                    grouped.forEach { (type, typeDevices) ->
                        val isExpanded = type in expandedTypes
                        item(key = "header_$type") {
                            DeviceGroupHeader(
                                typeName = type,
                                isExpanded = isExpanded,
                                onToggle = {
                                    expandedTypes = if (isExpanded) expandedTypes - type else expandedTypes + type
                                }
                            )
                        }
                        if (isExpanded) {
                            items(typeDevices, key = { "dev_${it.id}" }) { device ->
                                val isSelected = device.id in selectedIds
                                DeviceCard(
                                    device = device,
                                    isSelected = isSelected,
                                    isSelecting = isSelecting,
                                    onClick = {
                                        if (isSelecting) viewModel.toggleSelection(device.id)
                                        else navController.navigate(Screen.DeviceDetail.createRoute(device.id))
                                    },
                                    onLongClick = { viewModel.startSelection(device.id) },
                                    onEdit = { viewModel.showEditDialog(device) },
                                    onDelete = { viewModel.showDeleteConfirm(device) }
                                )
                            }
                        }
                    }
                } else {
                    items(devices.sortedBy { it.name }, key = { "dev_${it.id}" }) { device ->
                        val isSelected = device.id in selectedIds
                        DeviceCard(
                            device = device,
                            isSelected = isSelected,
                            isSelecting = isSelecting,
                            onClick = {
                                if (isSelecting) viewModel.toggleSelection(device.id)
                                else navController.navigate(Screen.DeviceDetail.createRoute(device.id))
                            },
                            onLongClick = { viewModel.startSelection(device.id) },
                            onEdit = { viewModel.showEditDialog(device) },
                            onDelete = { viewModel.showDeleteConfirm(device) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        DeviceDialog(deviceTypes = deviceTypeNames, onDismiss = { viewModel.hideAddDialog() }, onConfirm = { name, type, note -> viewModel.addDevice(name, type, note) })
    }
    if (showCreateBoxDialog) {
        CreateBoxDialog(
            devices = devices,
            onDismiss = { showCreateBoxDialog = false },
            onConfirm = { name, deviceId, rows, cols, note ->
                showCreateBoxDialog = false
                viewModel.createBox(name, rows, cols, note)
            }
        )
    }
    editingDevice?.let { device ->
        DeviceDialog(existing = device, deviceTypes = deviceTypeNames, onDismiss = { viewModel.hideEditDialog() }, onConfirm = { name, type, note -> viewModel.updateDevice(device.id, name, type, note) })
    }
    deletingDevice?.let { device ->
        DeleteConfirmDialog(
            message = stringResource(R.string.device_list_delete_confirm, device.name),
            onDismiss = { viewModel.hideDeleteConfirm() },
            onConfirm = { viewModel.deleteDevice(device) }
        )
    }
    if (showDeleteBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBatchConfirm = false },
            title = { Text(stringResource(R.string.btn_confirm_delete)) },
            text = { Text(stringResource(R.string.device_list_delete_batch_confirm, selectedIds.size)) },
            confirmButton = { TextButton(onClick = { showDeleteBatchConfirm = false; viewModel.deleteSelected() }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteBatchConfirm = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}

@Composable
private fun RecentlyViewedSection(
    recentBoxes: List<RecentBox>,
    isSelecting: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onStartSelect: (Long) -> Unit,
    onClick: (Long) -> Unit
) {
    Column {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.device_list_recent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentBoxes, key = { it.id }) { box ->
                RecentBoxCard(
                    box = box,
                    isSelected = box.id in selectedIds,
                    isSelecting = isSelecting,
                    onClick = {
                        if (isSelecting) onToggleSelect(box.id)
                        else onClick(box.id)
                    },
                    onLongClick = { onStartSelect(box.id) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentBoxCard(
    box: RecentBox,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(160.dp).combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelecting) {
                Box(
                    modifier = Modifier.size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    box.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (box.deviceName != null || box.layerName != null) {
                    Text(
                        listOfNotNull(box.deviceName, box.layerName).joinToString(" > "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    device: StorageDeviceEntity,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelecting) {
                Box(
                    modifier = Modifier.size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .then(if (!isSelected) Modifier.clip(CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                device.note?.let { note ->
                    Spacer(Modifier.height(2.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!isSelecting) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.device_list_edit), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}


@Composable
private fun DeviceGroupHeader(
    typeName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.small
) {
    val displayName = when (typeName) {
        "FREEZER_M80" -> stringResource(R.string.device_list_type_freezer)
        "LIQUID_NITROGEN" -> stringResource(R.string.device_list_type_nitrogen)
        else -> typeName
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) stringResource(R.string.content_description_collapse) else stringResource(R.string.content_description_expand),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CreateBoxDialog(
    devices: List<StorageDeviceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, deviceId: Long, rows: Int, cols: Int, note: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf("9") }
    var cols by remember { mutableStateOf("9") }
    var note by remember { mutableStateOf("") }

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.box_dialog_title_add), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.box_dialog_label_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = rows, onValueChange = { rows = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.box_dialog_label_rows)) },
                        singleLine = true, modifier = Modifier.weight(1f), shape = fieldShape, colors = fieldColors
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = cols, onValueChange = { cols = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.box_dialog_label_cols)) },
                        singleLine = true, modifier = Modifier.weight(1f), shape = fieldShape, colors = fieldColors
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.label_note_optional)) },
                    maxLines = 3, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val r = rows.toIntOrNull() ?: return@TextButton
                    val c = cols.toIntOrNull() ?: return@TextButton
                    onConfirm(name.trim(), 0L, r, c, note.trim().ifEmpty { null })
                },
                enabled = name.isNotBlank() && rows.toIntOrNull() != null && cols.toIntOrNull() != null
            ) { Text(stringResource(R.string.btn_add), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
