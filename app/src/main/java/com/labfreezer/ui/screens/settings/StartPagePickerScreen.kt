package com.labfreezer.ui.screens.settings
import com.labfreezer.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DeviceCounts(
    val layerCount: Int,
    val boxCount: Int
)

private data class LayerCounts(
    val boxCount: Int,
    val sampleCount: Int
)

private enum class CornerStyle { ALL, TOP, BOTTOM, NONE }

private fun cornerStyleToShape(style: CornerStyle): Shape {
    val r = 12.dp
    return when (style) {
        CornerStyle.ALL -> RoundedCornerShape(r)
        CornerStyle.TOP -> RoundedCornerShape(topStart = r, topEnd = r)
        CornerStyle.BOTTOM -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        CornerStyle.NONE -> RoundedCornerShape(0.dp)
    }
}

sealed class PickerNode {
    data object MainStorage : PickerNode()
    data object MainTags : PickerNode()
    data class Device(val entity: StorageDeviceEntity) : PickerNode()
    data class Layer(val entity: StorageLayerEntity) : PickerNode()
    data class Box(val entity: StorageBoxEntity) : PickerNode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPagePickerScreen(
    onBack: () -> Unit,
    deviceRepo: StorageDeviceRepository,
    layerRepo: StorageLayerRepository,
    boxRepo: StorageBoxRepository,
    sampleRepo: SamplePositionRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<StorageDeviceEntity>>(emptyList()) }
    var layers by remember { mutableStateOf<Map<Long, List<StorageLayerEntity>>>(emptyMap()) }
    var boxes by remember { mutableStateOf<Map<Long, List<StorageBoxEntity>>>(emptyMap()) }
    var directBoxes by remember { mutableStateOf<Map<Long, List<StorageBoxEntity>>>(emptyMap()) }
    var deviceCounts by remember { mutableStateOf<Map<Long, DeviceCounts>>(emptyMap()) }
    var layerCounts by remember { mutableStateOf<Map<Long, LayerCounts>>(emptyMap()) }
    var boxSampleCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var expandedDeviceIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var expandedLayerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val currentSetting = remember { StartPagePreference.get(context) }
    var searchQuery by remember { mutableStateOf("") }

    var allLayers by remember { mutableStateOf<List<StorageLayerEntity>>(emptyList()) }
    var allBoxes by remember { mutableStateOf<List<StorageBoxEntity>>(emptyList()) }

    LaunchedEffect(searchQuery, devices) {
        if (searchQuery.isNotBlank() && devices.isNotEmpty()) {
            val allL = withContext(Dispatchers.IO) { layerRepo.getAll() }
            allLayers = allL
            val allB = withContext(Dispatchers.IO) { boxRepo.getAll() }
            allBoxes = allB
        } else if (searchQuery.isBlank()) {
            allLayers = emptyList()
            allBoxes = emptyList()
        }
    }

    val filteredDevices = remember(devices, searchQuery, allLayers, allBoxes) {
        if (searchQuery.isBlank()) {
            devices
        } else {
            val q = searchQuery.trim().lowercase()
            // Match device names
            val matchingDeviceIds = devices.filter { it.name.lowercase().contains(q) }.map { it.id }.toMutableSet()
            // Match layer names
            val matchingLayerDeviceIds = allLayers.filter { it.name.lowercase().contains(q) }.map { it.deviceId }.toSet()
            matchingDeviceIds.addAll(matchingLayerDeviceIds)
            // Match box names
            val matchingBoxLayerIds = allBoxes.filter { it.name.lowercase().contains(q) }.map { it.layerId }.toSet()
            val matchingBoxDeviceIds = allLayers.filter { it.id in matchingBoxLayerIds }.map { it.deviceId }.toSet()
            matchingDeviceIds.addAll(matchingBoxDeviceIds)
            if (matchingDeviceIds.isEmpty()) emptyList()
            else devices.filter { it.id in matchingDeviceIds }
        }
    }

    // Auto-expand matching devices/layers when searching
    LaunchedEffect(searchQuery, filteredDevices) {
        if (searchQuery.isNotBlank() && filteredDevices.isNotEmpty()) {
            val q = searchQuery.trim().lowercase()
            var firstDeviceSet = false
            for (device in filteredDevices) {
                // Check if device name matches
                val deviceMatches = device.name.lowercase().contains(q)
                // Check if any layer matches in this device
                val matchingLayers = allLayers.filter { it.deviceId == device.id && it.name.lowercase().contains(q) }
                // Check if any box matches in this device
                val matchingBoxLayerIds = allBoxes.filter {
                    allLayers.any { l -> l.id == it.layerId && l.deviceId == device.id } &&
                    it.name.lowercase().contains(q)
                }.map { it.layerId }.toSet()
                val matchingBoxDeviceIds = allLayers.filter { it.id in matchingBoxLayerIds }.map { it.deviceId }.toSet()

                val hasAnyMatch = deviceMatches || matchingLayers.isNotEmpty() || matchingBoxDeviceIds.contains(device.id)

                if (hasAnyMatch && !firstDeviceSet) {
                    // Load layers for this device
                    if (!layers.containsKey(device.id)) {
                        val loaded = withContext(Dispatchers.IO) { layerRepo.getByDeviceId(device.id) }
                        if (loaded.isNotEmpty()) {
                            layers = layers + (device.id to loaded)
                        }
                    }
                    expandedDeviceIds = expandedDeviceIds + device.id
                    firstDeviceSet = true

                    // Expand first matching layer
                    val firstMatchingLayer = matchingLayers.firstOrNull()
                    if (firstMatchingLayer != null && !boxes.containsKey(firstMatchingLayer.id)) {
                        val loadedBoxes = withContext(Dispatchers.IO) { boxRepo.getByLayerId(firstMatchingLayer.id) }
                        if (loadedBoxes.isNotEmpty()) {
                            boxes = boxes + (firstMatchingLayer.id to loadedBoxes)
                        }
                    }
                    if (firstMatchingLayer != null) {
                        expandedLayerIds = expandedLayerIds + firstMatchingLayer.id
                    }
                }
            }
        } else if (searchQuery.isBlank()) {
            expandedDeviceIds = emptySet()
            expandedLayerIds = emptySet()
        }
    }

    LaunchedEffect(Unit) {
        val devs = withContext(Dispatchers.IO) { deviceRepo.getAll() }
        devices = devs
        deviceCounts = devs.associate { dev ->
            val lc = layerRepo.countByDeviceId(dev.id)
            val bc = boxRepo.countByDeviceId(dev.id)
            dev.id to DeviceCounts(layerCount = lc, boxCount = bc)
        }
    }

    fun select(setting: StartPageSetting) {
        StartPagePreference.set(context, setting)
        onBack()
    }

    fun loadLayers(deviceId: Long) {
        scope.launch {
            if (layers.containsKey(deviceId)) {
                expandedDeviceIds = if (deviceId in expandedDeviceIds) expandedDeviceIds - deviceId else expandedDeviceIds + deviceId
                expandedLayerIds = emptySet()
            } else {
                val result = withContext(Dispatchers.IO) { layerRepo.getByDeviceId(deviceId) }
                layers = layers + (deviceId to result)
                // 加载直接挂载在设备下的盒子（通过 hidden layer）
                val direct = withContext(Dispatchers.IO) { boxRepo.getBoxesByDeviceDirect(deviceId) }
                directBoxes = directBoxes + (deviceId to direct)
                // 加载直接盒子的样本计数
                boxSampleCounts = boxSampleCounts + direct.associate { box ->
                    box.id to withContext(Dispatchers.IO) { sampleRepo.countByBoxId(box.id) }
                }
                layerCounts = layerCounts + result.associate { layer ->
                    val bc = boxRepo.countByLayerId(layer.id)
                    val sc = sampleRepo.countByLayerId(layer.id)
                    layer.id to LayerCounts(boxCount = bc, sampleCount = sc)
                }
                expandedDeviceIds = expandedDeviceIds + deviceId
                expandedLayerIds = emptySet()
            }
        }
    }

    fun loadBoxes(layerId: Long) {
        scope.launch {
            if (boxes.containsKey(layerId)) {
                expandedLayerIds = if (layerId in expandedLayerIds) expandedLayerIds - layerId else expandedLayerIds + layerId
            } else {
                val deviceId = devices.firstOrNull { d ->
                    layers[d.id]?.any { it.id == layerId } == true
                }?.id ?: return@launch
                val result = withContext(Dispatchers.IO) { boxRepo.getByLayerId(layerId) }
                boxes = boxes + (layerId to result)
                boxSampleCounts = boxSampleCounts + result.associate { box ->
                    box.id to sampleRepo.countByBoxId(box.id)
                }
                expandedLayerIds = expandedLayerIds + layerId
            }
        }
    }

    fun isSelected(setting: StartPageSetting): Boolean {
        return currentSetting.route == setting.route && currentSetting.id == setting.id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.start_page_picker_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.start_page_picker_search_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_description_clear))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.start_page_picker_section_main), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                val defaultLabel = stringResource(R.string.start_page_default_label)
                PickerItem(
                    icon = Icons.Default.Home,
                    label = defaultLabel,
                    selected = isSelected(StartPageSetting(defaultLabel, Screen.DeviceList.route)),
                    onClick = { select(StartPageSetting(defaultLabel, Screen.DeviceList.route)) }
                )
            }

            item {
                val tagsLabel = stringResource(R.string.tab_tags)
                PickerItem(
                    icon = Icons.AutoMirrored.Filled.Label,
                    label = tagsLabel,
                    selected = isSelected(StartPageSetting(tagsLabel, Screen.TagManage.route)),
                    onClick = { select(StartPageSetting(tagsLabel, Screen.TagManage.route)) }
                )
            }

            item {
                val searchLabel = stringResource(R.string.tab_search)
                PickerItem(
                    icon = Icons.Default.Search,
                    label = searchLabel,
                    selected = isSelected(StartPageSetting(searchLabel, Screen.Search.route)),
                    onClick = { select(StartPageSetting(searchLabel, Screen.Search.route)) }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.start_page_picker_section_device), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }

            items(filteredDevices, key = { "device_${it.id}" }) { device ->
                val allDeviceLayers = layers[device.id] ?: emptyList()
                val deviceLayers = if (searchQuery.isBlank()) allDeviceLayers
                    else allDeviceLayers.filter { it.name.lowercase().contains(searchQuery.trim().lowercase(), ignoreCase = true) }
                // Auto-expand devices with matches when searching
                if (searchQuery.isNotBlank() && device.id !in expandedDeviceIds && allDeviceLayers.isEmpty()) {
                    // Load layers for this device if not loaded
                }
                val isDeviceExpanded = device.id in expandedDeviceIds
                val dc = deviceCounts[device.id] ?: DeviceCounts(0, 0)

                var totalChildItems = 0
                if (isDeviceExpanded) {
                    val deviceDirectBoxes = directBoxes[device.id] ?: emptyList()
                    totalChildItems += deviceDirectBoxes.size
                    deviceLayers.forEach { layer ->
                        totalChildItems++
                        val layerBoxes = boxes[layer.id] ?: emptyList()
                        val filteredBoxes = if (searchQuery.isBlank()) layerBoxes
                            else layerBoxes.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        if (layer.id in expandedLayerIds) {
                            totalChildItems += filteredBoxes.size
                        }
                    }
                }
                val hasChildren = totalChildItems > 0

                val deviceShape = cornerStyleToShape(
                    if (isDeviceExpanded && hasChildren) CornerStyle.TOP else CornerStyle.ALL
                )
                PickerItem(
                    icon = Icons.Default.DeviceHub,
                    label = device.name,
                    subtitle = stringResource(R.string.start_page_picker_format_layer_box, dc.layerCount, dc.boxCount),
                    selected = isSelected(StartPageSetting(device.name, Screen.DeviceDetail.route, device.id)),
                    onClick = { select(StartPageSetting(device.name, Screen.DeviceDetail.route, device.id)) },
                    indent = 0,
                    expandable = true,
                    isExpanded = isDeviceExpanded,
                    onToggle = { loadLayers(device.id) },
                    shape = deviceShape
                )

                if (isDeviceExpanded) {
                    var childIndex = 0
                    // 直接挂载在设备下的盒子（通过 hidden layer）
                    val deviceDirectBoxes = directBoxes[device.id] ?: emptyList()
                    val filteredDirectBoxes = if (searchQuery.isBlank()) deviceDirectBoxes
                        else deviceDirectBoxes.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    filteredDirectBoxes.forEach { box ->
                        childIndex++
                        val isLastChild = childIndex == totalChildItems
                        val sc = boxSampleCounts[box.id] ?: 0
                        val boxShape = cornerStyleToShape(
                            if (isLastChild) CornerStyle.BOTTOM else CornerStyle.NONE
                        )
                        PickerItem(
                            icon = Icons.Default.Inventory2,
                            label = box.name,
                            subtitle = stringResource(R.string.start_page_picker_format_grid_sample, box.rows, box.cols, sc),
                            selected = isSelected(StartPageSetting("${device.name} > ${box.name}", Screen.BoxGrid.route, box.id)),
                            onClick = { select(StartPageSetting("${device.name} > ${box.name}", Screen.BoxGrid.route, box.id)) },
                            indent = 1,
                            shape = boxShape
                        )
                    }
                    // 层级
                    deviceLayers.forEach { layer ->
                        childIndex++
                        val isLastLayer = childIndex == totalChildItems
                        val lc = layerCounts[layer.id] ?: LayerCounts(0, 0)
                        val isLayerExpanded = layer.id in expandedLayerIds
                        val layerBoxes = boxes[layer.id] ?: emptyList()
                        val filteredBoxes = if (searchQuery.isBlank()) layerBoxes
                            else layerBoxes.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        val layerHasChildren = isLayerExpanded && filteredBoxes.isNotEmpty()

                        val layerShape = cornerStyleToShape(
                            when {
                                isLastLayer && !layerHasChildren -> CornerStyle.BOTTOM
                                isLastLayer && layerHasChildren -> CornerStyle.NONE
                                else -> CornerStyle.NONE
                            }
                        )
                        PickerItem(
                            icon = Icons.Default.Layers,
                            label = layer.name,
                            subtitle = stringResource(R.string.start_page_picker_format_box_sample, lc.boxCount, lc.sampleCount),
                            selected = isSelected(StartPageSetting("${device.name} > ${layer.name}", Screen.LayerDetail.route, layer.id)),
                            onClick = { select(StartPageSetting("${device.name} > ${layer.name}", Screen.LayerDetail.route, layer.id)) },
                            indent = 1,
                            expandable = true,
                            isExpanded = isLayerExpanded,
                            onToggle = { loadBoxes(layer.id) },
                            shape = layerShape
                        )

                        if (isLayerExpanded) {
                            filteredBoxes.forEach { box ->
                                childIndex++
                                val isLastBox = childIndex == totalChildItems
                                val sc = boxSampleCounts[box.id] ?: 0

                                val boxShape = cornerStyleToShape(
                                    if (isLastBox) CornerStyle.BOTTOM else CornerStyle.NONE
                                )
                                PickerItem(
                                    icon = Icons.Default.Inventory2,
                                    label = box.name,
                                    subtitle = stringResource(R.string.start_page_picker_format_grid_sample, box.rows, box.cols, sc),
                                    selected = isSelected(StartPageSetting("${device.name} > ${layer.name} > ${box.name}", Screen.BoxGrid.route, box.id)),
                                    onClick = { select(StartPageSetting("${device.name} > ${layer.name} > ${box.name}", Screen.BoxGrid.route, box.id)) },
                                    indent = 2,
                                    shape = boxShape
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
    indent: Int = 0,
    expandable: Boolean = false,
    isExpanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.small
) {
    val indentPadding = (indent * 32).dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp + indentPadding, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            if (expandable && onToggle != null) {
                IconButton(onClick = onToggle) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = if (isExpanded) stringResource(R.string.content_description_collapse) else stringResource(R.string.content_description_expand),
                        modifier = Modifier.size(16.dp).rotate(if (isExpanded) 90f else 0f),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
