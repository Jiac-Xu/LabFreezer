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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.ui.navigation.Screen

private val TAG_COLORS = listOf("#1565C0","#2E7D32","#E65100","#6A1B9A","#AD1457","#00838F","#4E342E","#37474F","#C62828","#283593","#00695C","#EF6C00","#7B1FA2","#0277BD","#558B2F","#D84315")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(navController: NavController, viewModel: TagManageViewModel = hiltViewModel()) {
    val tagsWithCount by viewModel.tagsWithCount.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingTag by viewModel.editingTag.collectAsStateWithLifecycle()
    val deletingTag by viewModel.deletingTag.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u6807\u7b7e\u7ba1\u7406", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier.padding(bottom = 88.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "\u6dfb\u52a0\u6807\u7b7e")
            }
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
                        Text("\u8bbe\u5907\u7ec4\u7ba1\u7406", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) TagDialog(onDismiss = { viewModel.hideAddDialog() }, onConfirm = { name, color -> viewModel.addTag(name, color) })
    editingTag?.let { tag -> TagDialog(existing = tag, onDismiss = { viewModel.hideEditDialog() }, onConfirm = { name, color -> viewModel.updateTag(tag.id, name, color) }) }
    deletingTag?.let { tag -> AlertDialog(onDismissRequest = { viewModel.hideDeleteConfirm() }, title = { Text("\u786e\u8ba4\u5220\u9664", fontWeight = FontWeight.SemiBold) }, text = { Text("\u786e\u8ba4\u5220\u9664\u6807\u7b7e\u300c${tag.name}\u300d\uff1f") }, confirmButton = { TextButton(onClick = { viewModel.deleteTag(tag) }) { Text("\u5220\u9664", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { viewModel.hideDeleteConfirm() }) { Text("\u53d6\u6d88") } }) }
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
                Text("${sampleCount}\u6837\u672c", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "\u7f16\u8f91", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "\u5220\u9664", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
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
        title = { Text(if (existing != null) "\u7f16\u8f91\u6807\u7b7e" else "\u65b0\u5efa\u6807\u7b7e", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("\u6807\u7b7e\u540d\u79f0") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = fieldShape, colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                Text("\u9009\u62e9\u989c\u8272", style = MaterialTheme.typography.bodyMedium)
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
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), color) }, enabled = name.isNotBlank()) { Text(if (existing != null) "\u4fdd\u5b58" else "\u6dfb\u52a0", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") } }
    )
}

