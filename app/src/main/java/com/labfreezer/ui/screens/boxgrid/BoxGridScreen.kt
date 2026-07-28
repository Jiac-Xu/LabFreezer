package com.labfreezer.ui.screens.boxgrid
import com.labfreezer.R

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.labfreezer.data.search.ScopeType
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.sample.BrowseContextStore
import com.labfreezer.ui.screens.sample.SampleBrowseContext
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxGridScreen(
    navController: NavController,
    boxId: Long,
    viewModel: BoxGridViewModel = hiltViewModel()
) {
    LaunchedEffect(boxId) { viewModel.loadBox(boxId) }

    val box by viewModel.box.collectAsStateWithLifecycle()
    val cellsState by viewModel.cells.collectAsStateWithLifecycle()
    val cells = cellsState.list
    val pendingInput by viewModel.pendingInput.collectAsStateWithLifecycle()
    val inputMode by viewModel.inputMode.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    var showDeleteBatchConfirm by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraResult(success)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createPhotoUri()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.box_grid_camera_permission), Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onGalleryResult(uri)
    }

    LaunchedEffect(pendingInput) {
        pendingInput?.let { input ->
            when (input.mode) {
                InputMode.CAMERA -> {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = viewModel.createPhotoUri()
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                InputMode.GALLERY -> {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                InputMode.TEXT -> {
                    // 文字模式：直接导航到编辑页
                    input.sampleId.let { sampleId ->
                        val sampleIds = cells.filter { it.sampleId != null }.map { it.sampleId!! }
                        val boxCtx = SampleBrowseContext.Box(
                            boxId = boxId,
                            boxName = box?.name ?: "",
                            sampleIds = sampleIds
                        )
                        val ctxKey = BrowseContextStore.put(boxCtx)
                        navController.navigate(Screen.SampleEdit.createRoute(sampleId, ctxKey))
                    }
                    viewModel.clearPendingInput()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text(stringResource(R.string.box_grid_selected_count, selectedIds.size), fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(box?.name ?: "", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        val sampleCount = cells.count { it.sampleId != null }
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text(if (selectedIds.size == sampleCount) stringResource(R.string.box_grid_deselect_all) else stringResource(R.string.box_grid_select_all), fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = {
                            MoveState.selectedItemIds = selectedIds
                            MoveState.moveTarget = MoveTarget.BOX
                            MoveState.sourceBoxId = boxId
                            navController.navigate(Screen.MoveBrowser.route)
                        }) {
                            Icon(Icons.Default.OpenWith, contentDescription = stringResource(R.string.box_grid_move), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteBatchConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Box {
                            TextButton(onClick = { showModeMenu = true }) {
                                val (icon, label) = inputModeIconAndLabel(inputMode)
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = showModeMenu,
                                onDismissRequest = { showModeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.box_grid_input_mode_camera)) },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    onClick = { viewModel.setInputMode(InputMode.CAMERA); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.box_grid_input_mode_gallery)) },
                                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                    onClick = { viewModel.setInputMode(InputMode.GALLERY); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.box_grid_input_mode_text)) },
                                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                    onClick = { viewModel.setInputMode(InputMode.TEXT); showModeMenu = false }
                                )
                            }
                        }
                        IconButton(onClick = {
                            navController.navigate(Screen.Search.createRoute(
                                scopeType = ScopeType.BOX,
                                scopeId = boxId,
                                scopeName = box?.name
                            ))
                        }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.device_list_search))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (box == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.box_grid_loading), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val cols = box!!.cols
            var visibleCols by remember(cols) { mutableFloatStateOf(cols.toFloat()) }

            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val cellWidth = screenWidth / visibleCols
            val totalGridWidth = cellWidth * cols

            val showDetails by remember {
                derivedStateOf { visibleCols <= 5f }
            }

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.width(totalGridWidth).padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = cells,
                            key = { cell -> cell.row * 1000 + cell.col },
                            contentType = { cell -> cell.status }
                        ) { cell ->
                            val currentOnClick: () -> Unit = remember(cell, isSelecting, selectedIds) {
                                {
                                    if (isSelecting) {
                                        cell.sampleId?.let { viewModel.toggleSelection(it) }
                                    } else {
                                        if (cell.status == GridCellStatus.EMPTY) {
                                            viewModel.onCellClick(cell)
                                        } else {
                                            cell.sampleId?.let { sampleId ->
                                                // 创建盒子浏览上下文：提取所有有样本的单元格的 sampleId，按 grid 顺序排列
                                                val sampleIds = cells.filter { it.sampleId != null }.map { it.sampleId!! }
                                                val boxCtx = SampleBrowseContext.Box(
                                                    boxId = boxId,
                                                    boxName = box?.name ?: "",
                                                    sampleIds = sampleIds
                                                )
                                                val ctxKey = BrowseContextStore.put(boxCtx)
                                                navController.navigate(Screen.SampleEdit.createRoute(sampleId, ctxKey))
                                            }
                                        }
                                    }
                                    Unit
                                }
                            }

                            val currentOnLongClick: () -> Unit = remember(cell.sampleId) {
                                {
                                    cell.sampleId?.let { viewModel.startSelection(it) }
                                    Unit
                                }
                            }

                            GridCellView(
                                cell = cell,
                                isSelected = cell.sampleId != null && cell.sampleId in selectedIds,
                                isSelecting = isSelecting,
                                showDetails = showDetails,
                                onClick = currentOnClick,
                                onLongClick = currentOnLongClick
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().height(64.dp)
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.content_description_zoom_out), modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(12.dp))
                                Slider(
                                    value = visibleCols,
                                    onValueChange = { visibleCols = it },
                                    valueRange = 3f..max(3f, cols.toFloat()),
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.content_description_zoom_in), modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBatchConfirm = false },
            title = { Text(stringResource(R.string.btn_confirm_delete)) },
            text = { Text(stringResource(R.string.box_grid_confirm_delete, selectedIds.size)) },
            confirmButton = { TextButton(onClick = { showDeleteBatchConfirm = false; viewModel.deleteSelected() }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteBatchConfirm = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}

@Composable
private fun inputModeIconAndLabel(mode: InputMode): Pair<ImageVector, String> = when (mode) {
    InputMode.CAMERA -> Icons.Default.CameraAlt to stringResource(R.string.box_grid_input_mode_camera)
    InputMode.GALLERY -> Icons.Default.PhotoLibrary to stringResource(R.string.box_grid_input_mode_gallery)
    InputMode.TEXT -> Icons.Default.EditNote to stringResource(R.string.box_grid_input_mode_text)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCellView(
    cell: GridCell,
    isSelected: Boolean,
    isSelecting: Boolean,
    showDetails: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        cell.status == GridCellStatus.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
        cell.status == GridCellStatus.PHOTO_ONLY -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).then(
            if (isSelecting && cell.sampleId != null) Modifier.clickable { onClick() }
            else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (cell.status) {
                GridCellStatus.EMPTY -> {
                    Text(text = cell.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GridCellStatus.PHOTO_ONLY -> {
                    if (cell.photoPath != null) {
                        AsyncImage(model = Uri.parse(cell.photoPath), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                GridCellStatus.COMPLETE -> {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (cell.photoPath != null) {
                            AsyncImage(model = Uri.parse(cell.photoPath), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Text(text = cell.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showDetails && cell.status != GridCellStatus.EMPTY,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(4.dp)
                ) {
                    Column {
                        cell.sampleName?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(cell.label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            if (isSelecting && cell.sampleId != null) {
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(22.dp).clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f))
                        .then(if (!isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
