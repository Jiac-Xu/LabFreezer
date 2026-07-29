package com.labfreezer.ui.screens.move
import com.labfreezer.R

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.labfreezer.ui.screens.move.MoveBrowserViewModel.SearchResult
import com.labfreezer.ui.screens.move.MoveState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveBrowserScreen(
    onBack: () -> Unit,
    viewModel: MoveBrowserViewModel = hiltViewModel()
) {
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val boxes by viewModel.boxes.collectAsState()
    val gridCells by viewModel.gridCells.collectAsState()
    val selectedPositions by viewModel.selectedPositions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val moveCompleted by viewModel.moveCompleted.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()
    val selectedCount = MoveState.selectedItemIds.size

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                MoveState.selectMode -> when (MoveState.moveTarget) {
                                    MoveTarget.DEVICE -> stringResource(R.string.move_title_select_device)
                                    MoveTarget.LAYER -> stringResource(R.string.move_title_select_layer)
                                    MoveTarget.BOX -> stringResource(R.string.move_title_select_box)
                                    MoveTarget.CONTAINER -> stringResource(R.string.move_title_select_device)
                                }
                                else -> when (MoveState.moveTarget) {
                                    MoveTarget.DEVICE -> stringResource(R.string.move_action_layer)
                                    MoveTarget.LAYER -> stringResource(R.string.move_action_box)
                                    MoveTarget.BOX -> stringResource(R.string.move_action_sample)
                                    MoveTarget.CONTAINER -> stringResource(R.string.move_action_box)
                                }
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                        }
                    }
                )

                BreadcrumbBar(
                    items = breadcrumb,
                    onItemClick = { viewModel.navigateToBreadcrumb(it.level) }
                )

                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    isSearching = isSearching
                )

                if (isSearching) {
                    SearchResultsList(
                        results = searchResults,
                        onResultClick = { viewModel.navigateToSearchResult(it) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isSearching -> { }
                    currentLevel == MoveLevel.DEVICE -> {
                        DeviceList(
                            devices = devices,
                            onDeviceClick = { viewModel.navigateToDevice(it.id) }
                        )
                    }
                    currentLevel == MoveLevel.LAYER -> {
                        LayerList(
                            layers = layers,
                            onLayerClick = { viewModel.navigateToLayer(it.id) }
                        )
                    }
                    currentLevel == MoveLevel.BOX -> {
                        BoxList(
                            boxes = boxes,
                            onBoxClick = { viewModel.navigateToBox(it.id) }
                        )
                    }
                    currentLevel == MoveLevel.GRID -> {
                        GridView(
                            cells = gridCells,
                            selectedPositions = selectedPositions,
                            onCellClick = { cell ->
                                if (!cell.occupied) {
                                    viewModel.togglePosition(cell.row, cell.col)
                                }
                            },
                            selectedCount = selectedCount
                        )
                    }
                }
            }

            ConfirmButton(
                enabled = viewModel.canConfirm(),
                label = when {
                    MoveState.selectMode -> stringResource(R.string.move_confirm_target)
                    MoveState.moveTarget == MoveTarget.BOX && currentLevel == MoveLevel.GRID ->
                        stringResource(R.string.move_confirm_move_count, selectedCount)
                    (MoveState.moveTarget == MoveTarget.LAYER || MoveState.moveTarget == MoveTarget.CONTAINER) && currentLevel == MoveLevel.DEVICE -> {
                        if (selectedDeviceId != null) stringResource(R.string.move_confirm_to_device)
                        else stringResource(R.string.move_confirm_to_root)
                    }
                    else -> stringResource(R.string.move_confirm_move)
                },
                onClick = { viewModel.confirmMove(onBack) }
            )
        }
    }
}

@Composable
private fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (BreadcrumbItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (items.isEmpty()) {
            Text(stringResource(R.string.move_filter_all), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        items.forEachIndexed { index, item ->
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (index == items.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onItemClick(item) }
            )
            if (index < items.lastIndex) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(horizontal = 4.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.move_search_placeholder), style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.content_description_clear), modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(results) { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResultClick(result) },
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (result.level) {
                        MoveLevel.DEVICE -> Icons.Default.DeviceHub
                        MoveLevel.LAYER -> Icons.Default.Layers
                        MoveLevel.BOX -> Icons.Default.Inventory2
                        else -> Icons.Default.DeviceHub
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(result.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (result.parentLabel.isNotEmpty()) {
                            Text(result.parentLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<com.labfreezer.data.db.entity.StorageDeviceEntity>,
    onDeviceClick: (com.labfreezer.data.db.entity.StorageDeviceEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(devices) { device ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onDeviceClick(device) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeviceHub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(device.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun LayerList(
    layers: List<com.labfreezer.data.db.entity.StorageLayerEntity>,
    onLayerClick: (com.labfreezer.data.db.entity.StorageLayerEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(layers) { layer ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onLayerClick(layer) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(layer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!layer.note.isNullOrBlank()) {
                            Text(layer.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun BoxList(
    boxes: List<com.labfreezer.data.db.entity.StorageBoxEntity>,
    onBoxClick: (com.labfreezer.data.db.entity.StorageBoxEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(boxes) { box ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onBoxClick(box) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(box.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${box.rows}x${box.cols}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun GridView(
    cells: List<GridCellInfo>,
    selectedPositions: Set<Pair<Int, Int>>,
    onCellClick: (GridCellInfo) -> Unit,
    selectedCount: Int
) {
    val cols = if (cells.isNotEmpty()) (cells.maxOf { it.col } + 1) else 1

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.move_instruction, selectedPositions.size, selectedCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(cells, key = { it.row * 1000 + it.col }) { cell ->
                val isSelected = (cell.row to cell.col) in selectedPositions
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .then(
                            if (!cell.occupied) Modifier.clickable { onCellClick(cell) }
                            else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            cell.occupied -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cell.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                cell.occupied -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (cell.occupied) {
                            Text(
                                text = "✕",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.TopEnd).padding(1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmButton(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}
