package com.labfreezer.ui.screens.sample

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.labfreezer.ui.NavAnimState
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.move.MoveState
import com.labfreezer.ui.screens.move.MoveTarget
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SampleEditScreen(
    navController: NavController,
    sampleId: Long,
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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(viewModel.createPhotoUri())
        } else {
            Toast.makeText(context, "\u9700\u8981\u76f8\u673a\u6743\u9650\u624d\u80fd\u62cd\u7167", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { nextSampleId ->
            navController.navigate(Screen.SampleEdit.createRoute(nextSampleId)) {
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

    LaunchedEffect(state.saved) { if (state.saved) navController.popBackStack() }
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

    Scaffold(
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
                    val s = state.sample
                    Text(
                        if (s != null) SampleEditViewModel.positionToLabel(s.row, s.col) else "\u6837\u672c\u7f16\u8f91",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u8fd4\u56de")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.save() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Save, contentDescription = "保存")
            }
        }
        ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
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
                            model = Uri.parse(state.photoPath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clickable { showFullImage = true },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(8.dp))
                                Text("\u6682\u65e0\u7167\u7247", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(viewModel.createPhotoUri())
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.photoPath != null) "\u91cd\u65b0\u62cd\u7167" else "\u62cd\u7167")
                }
                if (state.photoPath != null) {
                    TextButton(
                        onClick = { if (state.ocrEnabled) viewModel.runOcrNow() },
                        enabled = state.ocrEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (state.ocrEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "OCR 识别",
                            color = if (state.ocrEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
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
                        Text("\u4f4d\u7f6e", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        val pos = state.sample?.let { SampleEditViewModel.positionToLabel(it.row, it.col) } ?: ""
                        Text("${state.deviceName} > ${state.layerName} > ${state.boxName} > $pos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "\u66f4\u6362\u4f4d\u7f6e", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = state.name, onValueChange = { viewModel.updateName(it) }, label = { Text("\u6837\u672c\u540d\u79f0") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors)

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.date,
                onValueChange = { viewModel.updateDate(it) },
                label = { Text("\u65e5\u671f (\u4f8b\u5982 2024-03-15)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = fieldColors,
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d -> viewModel.updateDate("%04d-%02d-%02d".format(y, m + 1, d)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.CalendarToday, contentDescription = "\u9009\u62e9\u65e5\u671f") }
                }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = state.note, onValueChange = { viewModel.updateNote(it) }, label = { Text("\u5907\u6ce8") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors)

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("\u6807\u7b7e", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { navController.navigate(Screen.TagManage.route) }) {
                    Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("\u7ba1\u7406\u6807\u7b7e")
                }
            }

            Spacer(Modifier.height(4.dp))

            if (allTags.isEmpty()) {
                Text("\u6682\u65e0\u6807\u7b7e\uff0c\u70b9\u51fb\u53f3\u4e0a\u89d2\u201c\u7ba1\u7406\u6807\u7b7e\u201d\u521b\u5efa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
        }
    }

    if (showFullImage && state.photoPath != null) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = Uri.parse(state.photoPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("\u786e\u8ba4\u5220\u9664", fontWeight = FontWeight.SemiBold) }, text = { Text("\u786e\u8ba4\u5220\u9664\u6b64\u6837\u672c\uff1f\u6b64\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500\u3002") },
            confirmButton = { TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) { Text("\u5220\u9664", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("\u53d6\u6d88") } })
    }

}




