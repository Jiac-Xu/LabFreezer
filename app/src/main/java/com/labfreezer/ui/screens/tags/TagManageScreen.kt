package com.labfreezer.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.labfreezer.R
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.ui.components.SpeedDialFAB
import com.labfreezer.ui.navigation.Screen

private val TAG_COLORS = listOf("#1565C0","#2E7D32","#E65100","#6A1B9A","#AD1457","#00838F","#4E342E","#37474F","#C62828","#283593","#00695C","#EF6C00","#7B1FA2","#0277BD","#558B2F","#D84315")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(
    navController: NavController,
    showBackButton: Boolean = true,
    showFabPadding: Boolean = true,
    viewModel: TagManageViewModel = hiltViewModel()
) {
    val tagsWithCount by viewModel.tagsWithCount.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingTag by viewModel.editingTag.collectAsStateWithLifecycle()
    val deletingTag by viewModel.deletingTag.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_manage_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
            SpeedDialFAB(
                onCreatePrimary = { viewModel.showAddDialog() },
                showSecondButton = false,
                modifier = if (showFabPadding) Modifier.padding(bottom = 88.dp) else Modifier
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 100.dp)
        ) {
            if (tagsWithCount.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("\u6682\u65e0\u6807\u7b7e", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                            Spacer(Modifier.height(8.dp))
                            Text("\u70b9\u51fb\u53f3\u4e0b\u89d2 + \u6dfb\u52a0\u7b2c\u4e00\u4e2a\u6807\u7b7e", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            } else {
                items(tagsWithCount, key = { it.tag.id }) { tagWithCount ->
                    TagItem(tag = tagWithCount.tag, sampleCount = tagWithCount.sampleCount, onClick = { navController.navigate(Screen.TagDetail.createRoute(tagWithCount.tag.id)) }, onEdit = { viewModel.showEditDialog(tagWithCount.tag) }, onDelete = { viewModel.showDeleteConfirm(tagWithCount.tag) })
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.DeviceTypeManage.route) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeviceHub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.device_type_manage_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) TagDialog(onDismiss = { viewModel.hideAddDialog() }, onConfirm = { name, color -> viewModel.addTag(name, color) })
    editingTag?.let { tag -> TagDialog(existing = tag, onDismiss = { viewModel.hideEditDialog() }, onConfirm = { name, color -> viewModel.updateTag(tag.id, name, color) }) }
    deletingTag?.let { tag -> AlertDialog(onDismissRequest = { viewModel.hideDeleteConfirm() }, title = { Text(stringResource(R.string.btn_confirm_delete), fontWeight = FontWeight.SemiBold) }, text = { Text(stringResource(R.string.tag_manage_delete_confirm, tag.name)) }, confirmButton = { TextButton(onClick = { viewModel.deleteTag(tag) }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { viewModel.hideDeleteConfirm() }) { Text(stringResource(R.string.btn_cancel)) } }) }
}

@Composable
private fun TagItem(tag: TagEntity, sampleCount: Int = 0, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(tag.color))))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.tag_manage_sample_count, sampleCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.device_list_edit), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagDialog(existing: TagEntity? = null, onDismiss: () -> Unit, onConfirm: (name: String, color: String) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: TAG_COLORS[0]) }
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) stringResource(R.string.tag_manage_edit_tag) else stringResource(R.string.tag_manage_new_tag), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.tag_manage_label_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.tag_manage_select_color), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAG_COLORS.forEach { c ->
                        val isSelected = c == color
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(c)))
                                .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), color) }, enabled = name.isNotBlank()) { Text(if (existing != null) stringResource(R.string.btn_save) else stringResource(R.string.btn_add), fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

