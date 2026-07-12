package com.labfreezer.ui.screens.layers

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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
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
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LayerDetailScreen(navController: NavController, layerId: Long, viewModel: LayerDetailViewModel = hiltViewModel()) {
    LaunchedEffect(layerId) { viewModel.loadLayer(layerId) }
    val layer by viewModel.layer.collectAsStateWithLifecycle()
    val boxes by viewModel.boxes.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val layersByDevice by viewModel.layersByDevice.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingBox by viewModel.editingBox.collectAsStateWithLifecycle()
    val deletingBox by viewModel.deletingBox.collectAsStateWithLifecycle()
    var showDeleteBatchConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text("\u5df2\u9009 ${selectedIds.size} \u9879", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(layer?.name ?: "", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            Text(if (selectedIds.size == boxes.size) "\u5168\u4e0d\u9009" else "\u5168\u9009", fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = {
                            MoveState.selectedItemIds = selectedIds
                            MoveState.moveTarget = MoveTarget.LAYER
                            MoveState.sourceLayerId = layer?.id
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
                    Icon(Icons.Default.Add, contentDescription = "\u6dfb\u52a0\u51bb\u5b58\u76d2")
                }
            }
        }
    ) { padding ->
        if (boxes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("\u6682\u65e0\u51bb\u5b58\u76d2", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(boxes, key = { it.id }) { box ->
                    val isSelected = box.id in selectedIds
                    BoxCard(
                        box = box,
                        isSelected = isSelected,
                        isSelecting = isSelecting,
                        onClick = {
                            if (isSelecting) viewModel.toggleSelection(box.id)
                            else navController.navigate(Screen.BoxGrid.createRoute(box.id))
                        },
                        onLongClick = { viewModel.startSelection(box.id) },
                        onEdit = { viewModel.showEditDialog(box) },
                        onDelete = { viewModel.showDeleteConfirm(box) }
                    )
                }
            }
        }
    }

    if (showAddDialog) BoxDialog(navController = navController, onDismiss = { viewModel.hideAddDialog() }, onConfirm = { name, _, rows, cols, note -> viewModel.addBox(name, rows, cols, note) })
    editingBox?.let { box ->
        BoxDialog(
            existing = box,
            availableDevices = allDevices,
            layersByDevice = layersByDevice,
            currentLayerId = layer?.id ?: 0,
            navController = navController,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, layerId, rows, cols, note -> viewModel.updateBox(box.id, name, layerId, rows, cols, note) }
        )
    }
    deletingBox?.let { box -> DeleteConfirmDialog(message = "\u786e\u8ba4\u5220\u9664\u51bb\u5b58\u76d2\u300c${box.name}\u300d\uff1f", onDismiss = { viewModel.hideDeleteConfirm() }, onConfirm = { viewModel.deleteBox(box) }) }
    if (showDeleteBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBatchConfirm = false },
            title = { Text("\u786e\u8ba4\u5220\u9664") },
            text = { Text("\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u7684 ${selectedIds.size} \u4e2a\u51bb\u5b58\u76d2\uff1f\u8be5\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500\u3002") },
            confirmButton = { TextButton(onClick = { showDeleteBatchConfirm = false; viewModel.deleteSelected() }) { Text("\u5220\u9664", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteBatchConfirm = false }) { Text("\u53d6\u6d88") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxCard(
    box: StorageBoxEntity,
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(box.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text("${box.rows}\u00d7${box.cols}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (!isSelecting) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "\u7f16\u8f91", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
                }
            }
            if (!isSelecting) {
                box.note?.let { note ->
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
