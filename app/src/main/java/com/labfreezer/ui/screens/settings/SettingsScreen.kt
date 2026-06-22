package com.labfreezer.ui.screens.settings

import android.content.Intent
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
import androidx.compose.material.icons.filled.PictureAsPdf
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
    ) { uri -> uri?.let { viewModel.importDatabase(it) } }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importCsv(it) } }

    val markdownImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importMarkdown(it) } }

    LaunchedEffect(exportResult) {
        exportResult?.let { r ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = r.mimeType
                putExtra(Intent.EXTRA_STREAM, r.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "\u5bfc\u51fa\u6570\u636e"))
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u8bbe\u7f6e", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))

            Text("  \u5916\u89c2", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            val themeLabel = when (currentThemeMode) {
                ThemeMode.LIGHT -> "\u6d45\u8272\u6a21\u5f0f"
                ThemeMode.DARK -> "\u6df1\u8272\u6a21\u5f0f"
                ThemeMode.SYSTEM -> "\u8ddf\u968f\u7cfb\u7edf"
            }
            val themeLabels = listOf("\u6d45\u8272\u6a21\u5f0f", "\u6df1\u8272\u6a21\u5f0f", "\u8ddf\u968f\u7cfb\u7edf")

            Box {
                SettingsCard(
                    icon = Icons.Filled.SettingsBrightness,
                    title = "\u4e3b\u9898\u6a21\u5f0f",
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
                title = "底栏编辑",
                subtitle = "自定义底栏标签顺序与可见性",
                onClick = onNavigateToBottomBarEdit
            )

            val startPageSetting = StartPagePreference.get(activity)
            SettingsCard(
                icon = Icons.Filled.Home,
                title = "\u542f\u52a8\u9875",
                subtitle = startPageSetting.label,
                onClick = onNavigateToStartPagePicker
            )

            Spacer(Modifier.height(16.dp))

            Text("  \u7ba1\u7406", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Filled.PhotoLibrary,
                title = "\u56fe\u7247\u6e05\u7406",
                subtitle = "\u6e05\u7406\u4e0d\u9700\u8981\u7684\u6837\u672c\u7167\u7247",
                onClick = onNavigateToImageCleanup
            )
            SettingsCard(
                icon = Icons.Filled.DocumentScanner,
                title = "OCR \u8bc6\u522b",
                subtitle = "\u67e5\u770b\u6a21\u578b\u72b6\u6001\u3001\u5f00\u5173\u53ca\u6279\u91cf\u6267\u884c",
                onClick = onNavigateToOcrSettings
            )

            Spacer(Modifier.height(16.dp))

            Text("  \u5e03\u5c40\u5bfc\u5165\u5bfc\u51fa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.TableChart,
                title = "CSV \u5bfc\u5165",
                subtitle = "\u4ece CSV \u6587\u4ef6\u5408\u5e76\u5bfc\u5165",
                onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values")) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.TableChart,
                title = "CSV \u5bfc\u51fa",
                subtitle = "\u5bfc\u51fa\u4e3a\u7535\u5b50\u8868\u683c\u683c\u5f0f",
                onClick = { viewModel.exportSample(ExportFormat.CSV) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.PictureAsPdf,
                title = "PDF \u5bfc\u51fa",
                subtitle = "\u542b\u7167\u7247\u7684\u7f51\u683c\u5e03\u5c40",
                onClick = { viewModel.exportSample(ExportFormat.PDF) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.Description,
                title = "Markdown \u5bfc\u51fa",
                subtitle = "\u542b\u56fe\u7247\u7684\u8868\u683c\uff0c\u53ef\u5916\u90e8\u4fee\u6539",
                onClick = { viewModel.exportSample(ExportFormat.MARKDOWN) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.Description,
                title = "Markdown \u5bfc\u5165",
                subtitle = "\u4ece .zip \u6587\u4ef6\u5bfc\u5165\u6837\u672c\u6570\u636e\u548c\u56fe\u7247",
                onClick = { markdownImportLauncher.launch(arrayOf("application/zip")) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )

            Spacer(Modifier.height(16.dp))

            Text("  \u6570\u636e\u5907\u4efd\u4e0e\u6062\u590d", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.CloudUpload,
                title = "\u5bfc\u51fa\u6570\u636e\u5e93",
                subtitle = "\u5907\u4efd\u4e3a .zip \u6570\u636e\u5305",
                onClick = { viewModel.exportDatabaseZip() },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )
            SettingsCard(
                icon = Icons.Default.CloudDownload,
                title = "\u5bfc\u5165\u6570\u636e\u5e93",
                subtitle = "\u4ece .zip \u6570\u636e\u5305\u6062\u590d",
                onClick = { importLauncher.launch(arrayOf("application/zip")) },
                trailing = { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            )

            Spacer(Modifier.height(16.dp))

            Text("  关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))

            val versionName = try {
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
            } catch (e: Exception) { "" }
            SettingsCard(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "版本 $versionName",
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
