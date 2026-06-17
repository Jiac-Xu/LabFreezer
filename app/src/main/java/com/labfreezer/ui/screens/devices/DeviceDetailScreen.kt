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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Layers
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeviceDetailScreen(navController: NavController, deviceId: Long, viewModel: DeviceDetailViewModel = hiltViewModel()) {
    LaunchedEffect(deviceId) { viewModel.loadDevice(deviceId) }
    val device by viewModel.device.collectAsStateWithLifecycle()
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingLayer by viewModel.editingLayer.collectAsStateWithLifecycle()
    val deletingLayer by viewModel.deletingLayer.collectAsStateWithLifecycle()

    var showDeleteBatchConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text("\u5df2\u9009 ${selectedIds.size} \u9879", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(device?.name ?: "", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { viewModel.exitSelection() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u53d6\u6d88")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u8fd4\u56de")
                        }
                    }
                },
                actions = {
                    if (isSelecting) {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text(if (selectedIds.size == layers.size) "\u5168\u4e0d\u9009" else "\u5168\u9009", fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = {
                            MoveState.selectedItemIds = selectedIds
                            MoveState.moveTarget = MoveTarget.DEVICE
                            MoveState.sourceDeviceId = device?.id
                            navController.navigate(Screen.MoveBrowser.route)
                        }) {
                            Icon(Icons.Default.OpenWith, contentDescription = "\u79fb\u52a8", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteBatchConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(Icons.Default.Search, contentDescription = "\u641c\u7d22")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        floatingActionButton = {
            if (!isSelecting) {
                FloatingActionButton(onClick = { viewModel.showAddDialog() }, shape = CircleShape, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.Default.Add, contentDescription = "\u6dfb\u52a0\u5c42")
                }
            }
        }
    ) { padding ->
        if (layers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("\u6682\u65e0\u5b58\u50a8\u5c42", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Text("\u70b9\u51fb\u53f3\u4e0b\u89d2 + \u6dfb\u52a0\u7b2c\u4e00\u5c42", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(layers, key = { it.id }) { layer ->
                    val isSelected = layer.id in selectedIds
                    LayerCard(
                        layer = layer,
                        isSelected = isSelected,
                        isSelecting = isSelecting,
                        onClick = {
                            if (isSelecting) viewModel.toggleSelection(layer.id)
                            else navController.navigate(Screen.LayerDetail.createRoute(layer.id))
                        },
                        onLongClick = { viewModel.startSelection(layer.id) },
                        onEdit = { viewModel.showEditDialog(layer) },
                        onDelete = { viewModel.showDeleteConfirm(layer) }
                    )
                }
            }
        }
    }

    if (showAddDialog) LayerDialog(navController = navController, onDismiss = { viewModel.hideAddDialog() }, onConfirm = { name, _, note -> viewModel.addLayer(name, note) })
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
    deletingLayer?.let { layer -> DeleteConfirmDialog(message = "\u786e\u8ba4\u5220\u9664\u5c42\u300c${layer.name}\u300d\uff1f", onDismiss = { viewModel.hideDeleteConfirm() }, onConfirm = { viewModel.deleteLayer(layer) }) }
    if (showDeleteBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBatchConfirm = false },
            title = { Text("\u786e\u8ba4\u5220\u9664") },
            text = { Text("\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u7684 ${selectedIds.size} \u4e2a\u5c42\uff1f\u8be5\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500\u3002") },
            confirmButton = { TextButton(onClick = { showDeleteBatchConfirm = false; viewModel.deleteSelected() }) { Text("\u5220\u9664", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteBatchConfirm = false }) { Text("\u53d6\u6d88") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LayerCard(
    layer: StorageLayerEntity,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(layer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                layer.note?.let { note -> Spacer(Modifier.height(4.dp)); Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (!isSelecting) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "\u7f16\u8f91", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
