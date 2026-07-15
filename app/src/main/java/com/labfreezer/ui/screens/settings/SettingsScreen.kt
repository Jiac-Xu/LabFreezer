package com.labfreezer.ui.screens.settings
import com.labfreezer.R

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labfreezer.ui.screens.export.ExportFormat
import com.labfreezer.ui.screens.export.ExportViewModel
import com.labfreezer.ui.theme.LocalThemeMode
import com.labfreezer.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeChanged: (ThemeMode) -> Unit = {},
    onNavigateToStartPagePicker: () -> Unit = {},
    onNavigateToBottomBarEdit: () -> Unit = {},
    onNavigateToImageCleanup: () -> Unit = {},
    onNavigateToOcrSettings: () -> Unit = {},
    onImportFileSelected: (Uri) -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: ExportViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val exportResult by viewModel.result.collectAsStateWithLifecycle()
    val activity = LocalContext.current
    val currentThemeMode = LocalThemeMode.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    var showThemeDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImportFileSelected(it) } }

    val markdownImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImportFileSelected(it) } }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importCsv(it) } }

    val dbExportSaveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportDatabaseToUri(it) } }

    LaunchedEffect(exportResult) {
        exportResult?.let { r ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = r.mimeType
                putExtra(Intent.EXTRA_STREAM, r.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.settings_export_db)))
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))

            Text("  " + stringResource(R.string.settings_section_appearance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            val themeLabel = when (currentThemeMode) {
                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
            }
            val themeLabels = listOf(stringResource(R.string.settings_theme_light), stringResource(R.string.settings_theme_dark), stringResource(R.string.settings_theme_system))

            Box {
                SettingsCard(
                    icon = Icons.Filled.SettingsBrightness,
                    title = stringResource(R.string.settings_theme_mode),
                    subtitle = themeLabel,
                    onClick = { showThemeDialog = true }
                )
                DropdownMenu(
                    expanded = showThemeDialog,
                    onDismissRequest = { showThemeDialog = false },
                    offset = DpOffset(x = screenWidth - 196.dp, y = 0.dp)
                ) {
                    themeLabels.forEachIndexed { index, label ->
                        val mode = ThemeMode.entries[index]
                        val isSelected = currentThemeMode == mode
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                showThemeDialog = false
                                onThemeChanged(mode)
                            }
                        )
                    }
                }
            }

            SettingsCard(
                icon = Icons.Filled.Edit,
                title = stringResource(R.string.settings_bottom_bar_edit),
                subtitle = stringResource(R.string.settings_bottom_bar_edit_subtitle),
                onClick = onNavigateToBottomBarEdit
            )

            val startPageSetting = StartPagePreference.get(activity)
            SettingsCard(
                icon = Icons.Filled.Home,
                title = stringResource(R.string.settings_start_page),
                subtitle = startPageSetting.label,
                onClick = onNavigateToStartPagePicker
            )

            Spacer(Modifier.height(16.dp))

            Text("  " + stringResource(R.string.settings_section_management), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Filled.PhotoLibrary,
                title = stringResource(R.string.settings_image_cleanup),
                subtitle = stringResource(R.string.settings_image_cleanup_subtitle),
                onClick = onNavigateToImageCleanup
            )
            SettingsCard(
                icon = Icons.Filled.DocumentScanner,
                title = stringResource(R.string.settings_ocr_settings),
                subtitle = stringResource(R.string.settings_ocr_settings_subtitle),
                onClick = onNavigateToOcrSettings
            )

            Spacer(Modifier.height(16.dp))

            Text("  " + stringResource(R.string.settings_section_import_export), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.TableChart,
                title = stringResource(R.string.settings_csv_import),
                subtitle = stringResource(R.string.settings_csv_import_subtitle),
                onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values")) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.TableChart,
                title = stringResource(R.string.settings_csv_export),
                subtitle = stringResource(R.string.settings_csv_export_subtitle),
                onClick = { viewModel.exportSample(ExportFormat.CSV) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_markdown_export),
                subtitle = stringResource(R.string.settings_markdown_export_subtitle),
                onClick = { viewModel.exportSample(ExportFormat.MARKDOWN) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_markdown_import),
                subtitle = stringResource(R.string.settings_markdown_import_subtitle),
                onClick = { markdownImportLauncher.launch(arrayOf("application/zip")) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )

            Spacer(Modifier.height(16.dp))

            Text("  " + stringResource(R.string.settings_section_data_backup), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.settings_export_db),
                subtitle = stringResource(R.string.settings_export_db_subtitle),
                onClick = { viewModel.exportDatabaseZip() },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.Save,
                title = stringResource(R.string.settings_export_db_save_as),
                subtitle = stringResource(R.string.settings_export_db_save_as_subtitle),
                onClick = {
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    dbExportSaveAsLauncher.launch("labfreezer_$ts.zip")
                },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.CloudDownload,
                title = stringResource(R.string.settings_import_db),
                subtitle = stringResource(R.string.settings_import_db_subtitle),
                onClick = { importLauncher.launch(arrayOf("application/zip")) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            Spacer(Modifier.height(16.dp))

            Text("  " + stringResource(R.string.settings_section_about), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            val versionName = try {
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
            } catch (e: Exception) { "" }
            SettingsCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_subtitle, versionName),
                onClick = onNavigateToAbout
            )

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing?.invoke()
        }
    }
}
