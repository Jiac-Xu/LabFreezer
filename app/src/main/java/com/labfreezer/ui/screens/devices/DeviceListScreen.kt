package com.labfreezer.ui.screens.devices

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
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
import com.labfreezer.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeviceListScreen(
    navController: NavController,
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
        isSelecting -> "\u5df2\u9009 ${selectedIds.size} \u9879"
        isSelectingRecent -> "\u5df2\u9009 ${selectedRecentIds.size} \u9879"
        else -> "\u51b0\u76d2"
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u53d6\u6d88")
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
                                    (isSelecting && selectedIds.size == devices.size)) "\u5168\u4e0d\u9009" else "\u5168\u9009",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = {
                            if (isSelectingRecent) viewModel.deleteSelectedRecent()
                            else showDeleteBatchConfirm = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(Icons.Default.Search, contentDescription = "\u641c\u7d22")
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
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    modifier = Modifier.padding(bottom = 88.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "\u6dfb\u52a0\u8bbe\u5907")
                }
            }
        }
    ) { padding ->
        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.DevicesOther, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("\u6682\u65e0\u5b58\u50a8\u8bbe\u5907", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Text("\u70b9\u51fb\u53f3\u4e0b\u89d2 + \u6dfb\u52a0\u7b2c\u4e00\u4e2a\u8bbe\u5907", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                    Text("设备", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                }
                val grouped = devices.groupBy { it.type }
                grouped.forEach { (type, typeDevices) ->
                    val isExpanded = type in expandedTypes
                    val deviceCount = typeDevices.size
                    val headerShape = if (isExpanded && deviceCount > 0) cornerStyleToShape(CornerStyle.TOP) else cornerStyleToShape(CornerStyle.ALL)
                    item(key = "header_$type") {
                        DeviceGroupHeader(
                            typeName = type,
                            isExpanded = isExpanded,
                            onToggle = {
                                expandedTypes = if (isExpanded) expandedTypes - type else expandedTypes + type
                            },
                            shape = headerShape
                        )
                    }
                    if (isExpanded) {
                        items(typeDevices, key = { "dev_${it.id}" }) { device ->
                            val idx = typeDevices.indexOf(device)
                            val isSelected = device.id in selectedIds
                            val cardShape: CornerStyle = when {
                                    deviceCount == 1 -> CornerStyle.ALL
                                    idx == 0 -> CornerStyle.NONE
                                    idx == deviceCount - 1 -> CornerStyle.BOTTOM
                                    else -> CornerStyle.NONE
                                }
                            val cardShape2 = cornerStyleToShape(cardShape)
                            DeviceCard(
                                device = device,
                                isSelected = isSelected,
                                isSelecting = isSelecting,
                                shape = cardShape2,
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
    }

    if (showAddDialog) {
        DeviceDialog(deviceTypes = deviceTypeNames, onDismiss = { viewModel.hideAddDialog() }, onConfirm = { name, type, note -> viewModel.addDevice(name, type, note) })
    }
    editingDevice?.let { device ->
        DeviceDialog(existing = device, deviceTypes = deviceTypeNames, onDismiss = { viewModel.hideEditDialog() }, onConfirm = { name, type, note -> viewModel.updateDevice(device.id, name, type, note) })
    }
    deletingDevice?.let { device ->
        DeleteConfirmDialog(
            message = "\u786e\u8ba4\u5220\u9664\u8bbe\u5907\u300c${device.name}\u300d\uff1f\n\u8be5\u8bbe\u5907\u4e0b\u7684\u6240\u6709\u5c42\u548c\u76d2\u5b50\u5c06\u88ab\u540c\u65f6\u5220\u9664\u3002",
            onDismiss = { viewModel.hideDeleteConfirm() },
            onConfirm = { viewModel.deleteDevice(device) }
        )
    }
    if (showDeleteBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBatchConfirm = false },
            title = { Text("\u786e\u8ba4\u5220\u9664") },
            text = { Text("\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u7684 ${selectedIds.size} \u4e2a\u8bbe\u5907\uff1f\u8be5\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500\u3002") },
            confirmButton = { TextButton(onClick = { showDeleteBatchConfirm = false; viewModel.deleteSelected() }) { Text("\u5220\u9664", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteBatchConfirm = false }) { Text("\u53d6\u6d88") } }
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
            Text("\u6700\u8fd1\u6d4f\u89c8", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
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
    onDelete: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = shape,
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
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "\u7f16\u8f91", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}


private enum class CornerStyle { ALL, TOP, BOTTOM, NONE }

private fun cornerStyleToShape(style: CornerStyle): androidx.compose.ui.graphics.Shape {
    val r = 12.dp
    return when (style) {
        CornerStyle.ALL -> RoundedCornerShape(r)
        CornerStyle.TOP -> RoundedCornerShape(topStart = r, topEnd = r)
        CornerStyle.BOTTOM -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        CornerStyle.NONE -> RoundedCornerShape(0.dp)
    }
}

@Composable
private fun DeviceGroupHeader(
    typeName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val displayName = when (typeName) {
        "FREEZER_M80" -> "-80°C 冰箱"
        "LIQUID_NITROGEN" -> "液氮罐"
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
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

