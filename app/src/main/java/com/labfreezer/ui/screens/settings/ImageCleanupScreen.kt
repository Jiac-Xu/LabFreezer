package com.labfreezer.ui.screens.settings
import com.labfreezer.R

import com.labfreezer.data.model.Position

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.labfreezer.data.db.dao.SampleWithPath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCleanupScreen(
    onBack: () -> Unit,
    viewModel: ImageCleanupViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBulkConfirm by remember { mutableStateOf(false) }
    var expandedDevice by remember { mutableStateOf<String?>(null) }
    var expandedLayer by remember { mutableStateOf<String?>(null) }
    var expandedBox by remember { mutableStateOf<String?>(null) }

    val groupedByDevice = groups.groupBy { it.deviceName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_cleanup_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back)) } },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.image_cleanup_delete_selected), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.image_cleanup_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { showBulkConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    enabled = !isDeleting
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.image_cleanup_clean_all), fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.image_cleanup_count, groups.sumOf { it.samples.size }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                groupedByDevice.forEach { (deviceName, deviceGroups) ->
                    val isDeviceExpanded = expandedDevice == deviceName
                    TreeSection(
                        label = deviceName,
                        icon = "device",
                        count = deviceGroups.sumOf { it.samples.size },
                        isExpanded = isDeviceExpanded,
                        onToggle = { expandedDevice = if (isDeviceExpanded) null else deviceName; expandedLayer = null; expandedBox = null }
                    )
                    if (isDeviceExpanded) {
                        deviceGroups.groupBy { it.layerName }.forEach { (layerName, layerGroups) ->
                            if (layerName.isBlank()) {
                                // 无层级（原 hidden 层）：盒子直接挂在设备下
                                layerGroups.groupBy { it.boxName }.forEach { (boxName, boxSamples) ->
                                    val isBoxExpanded = expandedBox == "direct_$boxName"
                                    TreeSection(
                                        label = boxName,
                                        icon = "box",
                                        count = boxSamples.sumOf { it.samples.size },
                                        isExpanded = isBoxExpanded,
                                        indent = 1,
                                        onToggle = { expandedBox = if (isBoxExpanded) null else "direct_$boxName" }
                                    )
                                    if (isBoxExpanded) {
                                        SamplePhotoGrid(
                                            samples = boxSamples.flatMap { it.samples },
                                            selectedIds = selectedIds,
                                            onToggle = { viewModel.toggleSelection(it) }
                                        )
                                    }
                                }
                            } else {
                                val isLayerExpanded = expandedLayer == layerName
                                TreeSection(
                                    label = layerName,
                                    icon = "layer",
                                    count = layerGroups.sumOf { it.samples.size },
                                    isExpanded = isLayerExpanded,
                                    indent = 1,
                                    onToggle = { expandedLayer = if (isLayerExpanded) null else layerName; expandedBox = null }
                                )
                                if (isLayerExpanded) {
                                    layerGroups.groupBy { it.boxName }.forEach { (boxName, boxSamples) ->
                                        val isBoxExpanded = expandedBox == boxName
                                        TreeSection(
                                            label = boxName,
                                            icon = "box",
                                            count = boxSamples.sumOf { it.samples.size },
                                            isExpanded = isBoxExpanded,
                                            indent = 2,
                                            onToggle = { expandedBox = if (isBoxExpanded) null else boxName }
                                        )
                                        if (isBoxExpanded) {
                                            SamplePhotoGrid(
                                                samples = boxSamples.flatMap { it.samples },
                                                selectedIds = selectedIds,
                                                onToggle = { viewModel.toggleSelection(it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    stringResource(R.string.image_cleanup_confirm_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.image_cleanup_confirm_delete_body, selectedIds.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelected { }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showBulkConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkConfirm = false },
            title = {
                Text(
                    stringResource(R.string.image_cleanup_clean_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.image_cleanup_confirm_clean_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkConfirm = false
                        viewModel.deleteAllNamedOrNoted { }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.image_cleanup_confirm_clean), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (isDeleting) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun TreeSection(
    label: String,
    icon: String,
    count: Int,
    isExpanded: Boolean,
    indent: Int = 0,
    onToggle: () -> Unit
) {
    val indentPadding = (indent * 28).dp
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = indentPadding, top = 6.dp, bottom = 6.dp).clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(
                when (icon) { "device" -> MaterialTheme.colorScheme.primary; "layer" -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.tertiary }
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = if (isExpanded) stringResource(R.string.content_description_collapse) else stringResource(R.string.content_description_expand),
            modifier = Modifier.size(16.dp).rotate(if (isExpanded) 90f else 0f),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SamplePhotoGrid(
    samples: List<SampleWithPath>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(((samples.size + 2) / 3 * 140).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(samples, key = { it.sampleId }) { sample ->
            val isSelected = sample.sampleId in selectedIds
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(sample.sampleId) },
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    if (sample.photoPath != null) {
                        AsyncImage(
                            model = Uri.parse(sample.photoPath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.image_cleanup_no_image), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (isSelected) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                        Box(
                            modifier = Modifier.padding(4.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)) {
                        sample.name?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Text(Position.toLabel(sample.row, sample.col), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}
