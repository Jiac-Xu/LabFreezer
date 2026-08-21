package com.labfreezer.ui.screens.sample
import com.labfreezer.R

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import com.labfreezer.ui.components.LabButton
import com.labfreezer.ui.components.LabButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.labfreezer.ui.NavAnimState
import com.labfreezer.ui.components.GlassScaffold
import com.labfreezer.ui.components.SpeedDialFAB
import com.labfreezer.ui.components.ZoomableImageViewer
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SampleEditScreen(
    navController: NavController,
    sampleId: Long,
    browseCtxKey: String? = null,
    viewModel: SampleEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> viewModel.onCameraResult(success) }

    var showFullImage by remember { mutableStateOf(false) }

    val imageModel = remember(state.photoPath, state.photoVersion) {
        state.photoPath?.let { path ->
            val uri = Uri.parse(path)
            if (uri.scheme == "file") {
                uri.buildUpon().appendQueryParameter("v", File(uri.path!!).lastModified().toString()).build()
            } else {
                uri
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(viewModel.createPhotoUri())
        } else {
            Toast.makeText(context, context.getString(R.string.box_grid_camera_permission), Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onGalleryResult(uri)
    }

    // Android 9 及以下保存到相册需要存储权限（10+ 走 MediaStore 无需权限）
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.savePhotoToGallery()
        } else {
            Toast.makeText(context, context.getString(R.string.save_photo_to_gallery_permission), Toast.LENGTH_SHORT).show()
        }
    }

    val saveToGallery: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.savePhotoToGallery()
        }
    }

    // 导航事件：携带 browseCtxKey 以保持上下文
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { nav ->
            navController.navigate(Screen.SampleEdit.createRoute(nav.sampleId, nav.browseCtxKey)) {
                popUpTo(Screen.SampleEdit.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 50.dp.toPx() }

    LaunchedEffect(state.deleted) { if (state.deleted) navController.popBackStack() }
    LaunchedEffect(MoveState.resultBoxId) {
        MoveState.resultBoxId?.let { boxId ->
            val row = MoveState.resultGridRow ?: return@let
            val col = MoveState.resultGridCol ?: return@let
            viewModel.setLocation(boxId, row, col)
            MoveState.resultBoxId = null
            MoveState.resultGridRow = null
            MoveState.resultGridCol = null
        }
    }

    // 生成浏览上下文副标题
    val browseSubtitle = browseCtxKey?.let { key ->
        BrowseContextStore.get(key)?.let { ctx ->
            buildBrowseSubtitle(ctx, state.currentIndex, state.totalCount, context)
        }
    }

    GlassScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount },
                onDragEnd = {
                    if (offsetX > threshold) {
                        NavAnimState.isSwipePrevious = true
                        viewModel.navigateAdjacent(isNext = false)
                    } else if (offsetX < -threshold) {
                        NavAnimState.isSwipePrevious = false
                        viewModel.navigateAdjacent(isNext = true)
                    }
                    offsetX = 0f
                },
                onDragCancel = { offsetX = 0f }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val s = state.sample
                        Text(
                            if (s != null) SampleEditViewModel.positionToLabel(s.row, s.col) else stringResource(R.string.sample_edit_title_fallback),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // 浏览上下文副标题：仅在有上下文且总样本数 > 1 时显示
                        if (browseSubtitle != null && state.totalCount > 1) {
                            Text(
                                text = browseSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            SpeedDialFAB(
                onCreatePrimary = { viewModel.save() },
                showSecondButton = false,
                primaryIcon = Icons.Default.Save,
                primaryButtonContentDescription = stringResource(R.string.content_description_save)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    if (state.photoPath != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clickable { showFullImage = true },
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { viewModel.rotatePhoto() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.RotateRight,
                                contentDescription = "Rotate",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.sample_edit_no_photo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabButton(
                        onClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(viewModel.createPhotoUri())
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = LabButtonDefaults.surfaceColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.photoPath != null) stringResource(R.string.sample_edit_retake_photo) else stringResource(R.string.sample_edit_take_photo))
                    }
                    LabButton(
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                        colors = LabButtonDefaults.surfaceColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sample_edit_gallery))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ocrEnabled = state.ocrEnabled && state.photoPath != null
                    LabButton(
                        onClick = { if (state.ocrEnabled) viewModel.runOcrNow() },
                        enabled = ocrEnabled,
                        modifier = Modifier.weight(1f),
                        colors = LabButtonDefaults.surfaceColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sample_edit_ocr_button))
                    }
                    val deletePhotoEnabled = state.photoPath != null
                    LabButton(
                        onClick = { viewModel.deletePhoto() },
                        enabled = deletePhotoEnabled,
                        modifier = Modifier.weight(1f),
                        colors = LabButtonDefaults.surfaceColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sample_edit_delete_photo))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        MoveState.selectMode = true
                        MoveState.moveTarget = MoveTarget.BOX
                        MoveState.resultBoxId = null
                        MoveState.resultGridRow = null
                        MoveState.resultGridCol = null
                        navController.navigate(Screen.MoveBrowser.route)
                    }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.sample_edit_label_location), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        val pos = state.sample?.let { SampleEditViewModel.positionToLabel(it.row, it.col) } ?: ""
                        val location = buildString {
                            if (state.deviceName.isNotBlank()) append(state.deviceName)
                            if (state.layerName.isNotBlank()) {
                                if (state.deviceName.isNotBlank()) append(" > ")
                                append(state.layerName)
                            }
                            append(" > ${state.boxName} > $pos")
                        }
                        Text(location, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = stringResource(R.string.sample_edit_change_location), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = state.name, onValueChange = { viewModel.updateName(it) }, label = { Text(stringResource(R.string.sample_edit_label_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors)

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.date,
                onValueChange = { viewModel.updateDate(it) },
                label = { Text(stringResource(R.string.sample_edit_label_date)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = fieldColors,
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d -> viewModel.updateDate("%04d-%02d-%02d".format(y, m + 1, d)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.sample_edit_pick_date)) }
                }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = state.note, onValueChange = { viewModel.updateNote(it) }, label = { Text(stringResource(R.string.sample_edit_label_note)) }, minLines = 1, maxLines = 8, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.sample_edit_section_tags), style = MaterialTheme.typography.titleSmall)
                TextButton(
                    onClick = { navController.navigate(Screen.TagManage.route) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.sample_edit_manage_tags))
                }
            }

            Spacer(Modifier.height(4.dp))

            if (allTags.isEmpty()) {
                Text(stringResource(R.string.sample_edit_no_tags), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allTags.forEach { tag ->
                        val selected = tag.id in state.assignedTagIds
                        Surface(
                            onClick = { viewModel.toggleTag(tag.id) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(tag.color)))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tag.name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(88.dp + padding.calculateBottomPadding()))
        }
    }

    if (showFullImage && state.photoPath != null) {
        ZoomableImageViewer(
            model = imageModel,
            onDismiss = { showFullImage = false },
            onSave = saveToGallery
        )
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text(stringResource(R.string.btn_confirm_delete), fontWeight = FontWeight.SemiBold) }, text = { Text(stringResource(R.string.sample_edit_confirm_delete)) },
            confirmButton = { TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.btn_cancel)) } })
    }

}

/**
 * 根据浏览上下文生成副标题文本。
 * 由 UI 根据 Context 的原始字段生成，支持国际化。
 */
@Composable
fun buildBrowseSubtitleText(
    context: SampleBrowseContext,
    currentIndex: Int,
    totalCount: Int
): String? {
    if (totalCount <= 1) return null
    return when (context) {
        is SampleBrowseContext.Box -> {
            stringResource(R.string.browse_context_box, context.boxName, currentIndex + 1, totalCount)
        }
        is SampleBrowseContext.Search -> {
            val filterSummary = buildFilterSummaryText(context.filterContext)
            if (filterSummary != null) {
                stringResource(R.string.browse_context_search_with_filters, context.query, filterSummary, currentIndex + 1, totalCount)
            } else {
                stringResource(R.string.browse_context_search, context.query, currentIndex + 1, totalCount)
            }
        }
        is SampleBrowseContext.Tag -> {
            stringResource(R.string.browse_context_tag, context.tagName, currentIndex + 1, totalCount)
        }
    }
}

/**
 * 生成筛选条件摘要文本。
 * 少量条件时显示具体值，大量条件时显示数量。
 */
@Composable
fun buildFilterSummaryText(filterContext: SearchFilterContext): String? {
    val conditions = filterContext.conditions
    if (conditions.isEmpty()) return null
    val totalFilterCount = conditions.sumOf { it.values.size }
    return if (totalFilterCount <= 3) {
        conditions.joinToString("+") { cond ->
            when (cond.type) {
                FilterType.BOX -> cond.values.joinToString("/")
                FilterType.DATE -> cond.values.joinToString("/")
                FilterType.TAG -> cond.values.joinToString("/")
            }
        }
    } else {
        stringResource(R.string.browse_context_filter_count, totalFilterCount)
    }
}

/**
 * 非 Composable 版本的副标题生成（用于需要 Context 的场景）。
 */
fun buildBrowseSubtitle(
    context: SampleBrowseContext,
    currentIndex: Int,
    totalCount: Int,
    androidContext: android.content.Context
): String? {
    if (totalCount <= 1) return null
    return when (context) {
        is SampleBrowseContext.Box -> {
            androidContext.getString(R.string.browse_context_box, context.boxName, currentIndex + 1, totalCount)
        }
        is SampleBrowseContext.Search -> {
            val filterSummary = buildFilterSummary(context.filterContext, androidContext)
            if (filterSummary != null) {
                androidContext.getString(R.string.browse_context_search_with_filters, context.query, filterSummary, currentIndex + 1, totalCount)
            } else {
                androidContext.getString(R.string.browse_context_search, context.query, currentIndex + 1, totalCount)
            }
        }
        is SampleBrowseContext.Tag -> {
            androidContext.getString(R.string.browse_context_tag, context.tagName, currentIndex + 1, totalCount)
        }
    }
}

/**
 * 非 Composable 版本的筛选条件摘要生成。
 */
fun buildFilterSummary(
    filterContext: SearchFilterContext,
    androidContext: android.content.Context
): String? {
    val conditions = filterContext.conditions
    if (conditions.isEmpty()) return null
    val totalFilterCount = conditions.sumOf { it.values.size }
    return if (totalFilterCount <= 3) {
        conditions.joinToString("+") { cond ->
            when (cond.type) {
                FilterType.BOX -> cond.values.joinToString("/")
                FilterType.DATE -> cond.values.joinToString("/")
                FilterType.TAG -> cond.values.joinToString("/")
            }
        }
    } else {
        androidContext.getString(R.string.browse_context_filter_count, totalFilterCount)
    }
}