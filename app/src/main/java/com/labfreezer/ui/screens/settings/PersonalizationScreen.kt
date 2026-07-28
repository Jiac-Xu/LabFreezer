package com.labfreezer.ui.screens.settings
import com.labfreezer.R

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.labfreezer.ui.theme.LocalThemeMode
import com.labfreezer.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreen(
    onBack: () -> Unit = {},
    onNavigateToStartPagePicker: () -> Unit = {},
    onNavigateToBottomBarEdit: () -> Unit = {},
    onThemeChanged: (ThemeMode) -> Unit = {},
    viewModel: PersonalizationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentThemeMode = LocalThemeMode.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.personalization_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))

            // ── 界面 (Appearance) ──
            Text("  " + stringResource(R.string.personalization_section_appearance),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            val themeLabel = when (currentThemeMode) {
                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
            }
            val themeLabels = listOf(
                stringResource(R.string.settings_theme_light),
                stringResource(R.string.settings_theme_dark),
                stringResource(R.string.settings_theme_system)
            )

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
                icon = Icons.Filled.Home,
                title = stringResource(R.string.settings_start_page),
                subtitle = StartPagePreference.get(LocalContext.current).label,
                onClick = onNavigateToStartPagePicker
            )
            SettingsCard(
                icon = Icons.Filled.Edit,
                title = stringResource(R.string.settings_bottom_bar_edit),
                subtitle = stringResource(R.string.settings_bottom_bar_edit_subtitle),
                onClick = onNavigateToBottomBarEdit
            )

            Spacer(Modifier.height(16.dp))

            // ── 录入 (Input) ──
            Text("  " + stringResource(R.string.personalization_section_input),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            val inputModeLabel = when (state.inputMode) {
                "GALLERY" -> stringResource(R.string.box_grid_input_mode_gallery)
                "TEXT" -> stringResource(R.string.box_grid_input_mode_text)
                else -> stringResource(R.string.box_grid_input_mode_camera)
            }
            var showInputModeDialog by remember { mutableStateOf(false) }
            Box {
                SettingsCard(
                    icon = Icons.Filled.CameraAlt,
                    title = stringResource(R.string.personalization_input_mode),
                    subtitle = stringResource(R.string.personalization_input_mode_subtitle) + " · $inputModeLabel",
                    onClick = { showInputModeDialog = true }
                )
                DropdownMenu(
                    expanded = showInputModeDialog,
                    onDismissRequest = { showInputModeDialog = false },
                    offset = DpOffset(x = screenWidth - 196.dp, y = 0.dp)
                ) {
                    val modes = listOf(
                        "CAMERA" to stringResource(R.string.box_grid_input_mode_camera) to Icons.Filled.CameraAlt,
                        "GALLERY" to stringResource(R.string.box_grid_input_mode_gallery) to Icons.Filled.PhotoLibrary,
                        "TEXT" to stringResource(R.string.box_grid_input_mode_text) to Icons.Filled.EditNote
                    )
                    modes.forEach { (pair, icon) ->
                        val (value, label) = pair
                        val isSelected = state.inputMode == value
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showInputModeDialog = false
                                viewModel.setInputMode(value)
                            }
                        )
                    }
                }
            }

            SettingsSwitchCard(
                icon = Icons.Filled.SwapHoriz,
                title = stringResource(R.string.personalization_temp_mode),
                subtitle = stringResource(R.string.personalization_temp_mode_subtitle),
                checked = state.tempModeAllowed,
                onCheckedChange = { viewModel.setTempModeAllowed(it) }
            )

            Spacer(Modifier.height(16.dp))

            // ── 编辑 (Editing) ──
            Text("  " + stringResource(R.string.personalization_section_edit),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            SettingsSwitchCard(
                icon = Icons.Filled.Save,
                title = stringResource(R.string.personalization_auto_save),
                subtitle = stringResource(R.string.personalization_auto_save_subtitle),
                checked = state.autoSaveEnabled,
                onCheckedChange = { viewModel.setAutoSaveEnabled(it) }
            )

            Spacer(Modifier.height(16.dp))

            // ── 浏览 (Browsing) ──
            Text("  " + stringResource(R.string.personalization_section_browsing),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            SettingsSwitchCard(
                icon = Icons.Filled.ZoomIn,
                title = stringResource(R.string.personalization_zoom_slider),
                subtitle = stringResource(R.string.personalization_zoom_slider_subtitle),
                checked = state.zoomSliderEnabled,
                onCheckedChange = { viewModel.setZoomSliderEnabled(it) }
            )

            Spacer(Modifier.height(16.dp))

            // ── 搜索 (Search) ──
            Text("  " + stringResource(R.string.personalization_section_search),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            SettingsSwitchCard(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.personalization_search_history),
                subtitle = stringResource(R.string.personalization_search_history_subtitle),
                checked = state.searchHistoryEnabled,
                onCheckedChange = { viewModel.setSearchHistoryEnabled(it) }
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
    onClick: () -> Unit
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
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}