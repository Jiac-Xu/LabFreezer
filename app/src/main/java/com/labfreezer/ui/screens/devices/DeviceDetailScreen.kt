@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.labfreezer.ui.screens.devices

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.labfreezer.R
import com.labfreezer.data.model.NodeType
import com.labfreezer.data.model.VisibleTreeNode
import com.labfreezer.data.search.ScopeType
import com.labfreezer.ui.components.SpeedDialFAB
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.layers.BoxDialog
import com.labfreezer.ui.screens.layers.DeleteConfirmDialog
import com.labfreezer.ui.screens.layers.LayerDialog
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget

@Composable
fun DeviceDetailScreen(navController: NavController, deviceId: Long, viewModel: DeviceDetailViewModel = hiltViewModel()) {
    LaunchedEffect(deviceId) { viewModel.loadDevice(deviceId) }
    val device by viewModel.device.collectAsStateWithLifecycle()
    val visibleChildren by viewModel.visibleChildren.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val addDialogMode by viewModel.addDialogMode.collectAsStateWithLifecycle()
    val canCreateLevel by viewModel.canCreateLevel.collectAsStateWithLifecycle()
    val editingLayer by viewModel.editingLayer.collectAsStateWithLifecycle()
    val deletingLayer by viewModel.deletingLayer.collectAsStateWithLifecycle()
    val editingBox by viewModel.editingBox.collectAsStateWithLifecycle()
    val deletingBox by viewModel.deletingBox.collectAsStateWithLifecycle()

    var showDeleteBatchConfirm by remember { mutableStateOf(false) }
    var speedDialExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text(stringResource(R.string.device_list_selected_count, selectedIds.size), fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(device?.name ?: "", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { viewModel.exitSelection() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                        }
                    }
                },
                actions = {
                    if (isSelecting) {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text(
                                if (selectedIds.size == visibleChildren.size) stringResource(R.string.device_list_deselect_all)
                                else stringResource(R.string.device_list_select_all),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = {
                            MoveState.selectedItemIds = selectedIds
                            MoveState.moveTarget = MoveTarget.DEVICE
                            MoveState.sourceDeviceId = device?.id
                            navController.navigate(Screen.MoveBrowser.route)
                        }) {
                            Icon(Icons.Default.OpenWith, contentDescription = stringResource(R.string.btn_save), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteBatchConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = {
                            navController.navigate(Screen.Search.createRoute(
                                scopeType = ScopeType.DEVICE,
                                scopeId = deviceId,
                                scopeName = device?.name
                            ))
                        }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.device_list_search))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        floatingActionButton = {
            if (!isSelecting) {
                SpeedDialFAB(
                    expanded = speedDialExpanded,
                    onToggle = { speedDialExpanded = !speedDialExpanded },
                    onCreateBox = { viewModel.showCreateBoxDialog() },
                    onCreateLevel = { viewModel.showCreateLevelDialog() },
                    showCreateLevel = canCreateLevel
                )
            }
        }
    ) { padding ->
        if (visibleChildren.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.device_list_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.device_list_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(visibleChildren, key = { "${it.type}_${it.id}" }) { node ->
                    val isSelected = node.id in selectedIds
                    VisibleChildCard(
                        node = node,
                        isSelected = isSelected,
                        isSelecting = isSelecting,
                        onClick = {
                            if (isSelecting) viewModel.toggleSelection(node.id)
                            else {
                                when (node.type) {
                                    NodeType.LEVEL -> navController.navigate(Screen.LayerDetail.createRoute(node.id))
                                    NodeType.BOX -> navController.navigate(Screen.BoxGrid.createRoute(node.id))
                                    else -> {}
                                }
                            }
                        },
                        onLongClick = { viewModel.startSelection(node.id) },
                        onEdit = {
                            when (node.type) {
                                NodeType.LEVEL -> {
                                    val layer = com.labfreezer.data.db.entity.StorageLayerEntity(
                                        id = node.id,
                                        deviceId = device?.id ?: 0,
                                        name = node.name
                                    )
                                    viewModel.showEditDialog(layer)
                                }
                                NodeType.BOX -> {
                                    viewModel.showEditBoxDialog(node.id)
                                }
                                else -> {}
                            }
                        },
                        onDelete = {
                            when (node.type) {
                                NodeType.LEVEL -> {
                                    val layer = com.labfreezer.data.db.entity.StorageLayerEntity(
                                        id = node.id,
                                        deviceId = device?.id ?: 0,
                                        name = node.name
                                    )
                                    viewModel.showDeleteConfirm(layer)
                                }
                                NodeType.BOX -> {
                                    viewModel.showDeleteBoxConfirm(node.id)
                                }
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    }

    // 创建盒子对话框
    if (showAddDialog && addDialogMode == com.labfreezer.ui.screens.devices.AddDialogMode.BOX) {
        BoxDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, _, rows, cols, note -> viewModel.addBox(name, rows, cols, note) }
        )
    }

    // 创建层级对话框
    if (showAddDialog && addDialogMode == com.labfreezer.ui.screens.devices.AddDialogMode.LEVEL) {
        LayerDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, note -> viewModel.addLayer(name, note) }
        )
    }

    editingLayer?.let { layer ->
        LayerDialog(
            existing = layer,
            availableDevices = allDevices,
            currentDeviceId = device?.id ?: 0,
            navController = navController,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, deviceId, note -> viewModel.updateLayer(layer.id, name, deviceId, note) }
        )
    }
    deletingLayer?.let { layer ->
        DeleteConfirmDialog(
            message = stringResource(R.string.device_list_delete_confirm, layer.name),
            onDismiss = { viewModel.hideDeleteConfirm() },
            onConfirm = { viewModel.deleteLayer(layer) }
        )
    }
    editingBox?.let { box ->
        BoxDialog(
            existing = box,
            navController = null,
            onDismiss = { viewModel.hideEditBoxDialog() },
            onConfirm = { name, _, rows, cols, note -> viewModel.updateBox(box.id, name, box.layerId, rows, cols, note) }
        )
    }
    deletingBox?.let { box ->
        DeleteConfirmDialog(
            message = stringResource(R.string.device_list_delete_confirm, box.name),
            onDismiss = { viewModel.hideDeleteBoxConfirm() },
            onConfirm = { viewModel.deleteBox(box) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VisibleChildCard(
    node: VisibleTreeNode,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelecting) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            // 根据类型显示不同图标
            val icon = when (node.type) {
                NodeType.FREEZER -> Icons.Default.DeviceHub
                NodeType.LEVEL -> Icons.Default.Layers
                NodeType.BOX -> Icons.Default.Inventory2
            }
            val iconTint = when (node.type) {
                NodeType.FREEZER -> MaterialTheme.colorScheme.primary
                NodeType.LEVEL -> MaterialTheme.colorScheme.secondary
                NodeType.BOX -> MaterialTheme.colorScheme.tertiary
            }
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!isSelecting) {
                Spacer(Modifier.width(8.dp))
                if (node.type == NodeType.LEVEL || node.type == NodeType.BOX) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.device_list_edit), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
