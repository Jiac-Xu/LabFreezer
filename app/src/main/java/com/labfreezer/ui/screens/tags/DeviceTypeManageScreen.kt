package com.labfreezer.ui.screens.tags
import com.labfreezer.R

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labfreezer.data.db.entity.DeviceTypeEntity
import com.labfreezer.ui.components.SpeedDialFAB
import com.labfreezer.ui.glass.GlassSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTypeManageScreen(
    onBack: () -> Unit,
    viewModel: TagManageViewModel = hiltViewModel()
) {
    val deviceTypes by viewModel.deviceTypes.collectAsStateWithLifecycle()
    val showAddTypeDialog by viewModel.showAddTypeDialog.collectAsStateWithLifecycle()
    val deletingType by viewModel.deletingType.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.device_type_manage_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
                onCreatePrimary = { viewModel.showAddTypeDialog() },
                showSecondButton = false
            )
        }
    ) { padding ->
        if (deviceTypes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.device_type_manage_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.device_type_manage_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    val context = LocalContext.current
                    val prefs = context.getSharedPreferences("device_group_prefs", android.content.Context.MODE_PRIVATE)
                    var groupByType by remember { mutableStateOf(prefs.getBoolean("group_by_type_enabled", true)) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.device_type_manage_group_toggle), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            GlassSwitch(
                                checked = groupByType,
                                onCheckedChange = {
                                    groupByType = it
                                    prefs.edit().putBoolean("group_by_type_enabled", it).apply()
                                }
                            )
                        }
                    }
                }
                items(deviceTypes, key = { it.id }) { type ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeviceHub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(type.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.showDeleteTypeConfirm(type) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    if (showAddTypeDialog) {
        var typeName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.hideAddTypeDialog() },
            title = { Text(stringResource(R.string.device_type_manage_dialog_title), fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = typeName,
                    onValueChange = { typeName = it },
                    label = { Text(stringResource(R.string.device_type_manage_dialog_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.addDeviceType(typeName.trim()) }, enabled = typeName.isNotBlank()) { Text(stringResource(R.string.btn_add), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { viewModel.hideAddTypeDialog() }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
    deletingType?.let { type ->
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteTypeConfirm() },
            title = { Text(stringResource(R.string.btn_confirm_delete), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.device_type_manage_delete_confirm, type.name)) },
            confirmButton = { TextButton(onClick = { viewModel.deleteDeviceType(type) }) { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { viewModel.hideDeleteTypeConfirm() }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}
