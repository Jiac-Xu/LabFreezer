package com.labfreezer.ui.screens.search

import com.labfreezer.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.search.ScopeType
import com.labfreezer.data.search.SearchScope
import com.labfreezer.data.search.SearchHistoryItem
import com.labfreezer.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    showBackButton: Boolean = true,
    scope: SearchScope = SearchScope(ScopeType.ALL),
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val facetGroups by viewModel.facetGroups.collectAsStateWithLifecycle()
    val activeFacetIds by viewModel.activeFacetIds.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    // 设置搜索范围
    LaunchedEffect(scope) {
        viewModel.setScope(scope)
    }

    LaunchedEffect(Unit) { if (query.isEmpty()) focusRequester.requestFocus() }
    DisposableEffect(Unit) { onDispose { viewModel.saveCurrentQuery() } }

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )

    // 动态 placeholder
    val placeholderText = when (scope.type) {
        ScopeType.ALL -> stringResource(R.string.search_placeholder_all)
        else -> {
            val name = scope.name ?: ""
            if (name.length > 20) stringResource(R.string.search_placeholder_scope, name.take(18) + "…")
            else stringResource(R.string.search_placeholder_scope, name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title), fontWeight = FontWeight.SemiBold) },
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
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text(placeholderText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { viewModel.onQueryChange("") }) { Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.content_description_clear)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                shape = fieldShape,
                colors = fieldColors
            )

            // 动态筛选区域（有结果且有筛选条件时显示）
            if (facetGroups.isNotEmpty() && results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FacetedFilterRow(
                    facetGroups = facetGroups,
                    onToggleFacet = { viewModel.toggleFacet(it) },
                    onClearAll = { viewModel.clearFacets() },
                    hasActiveFacets = activeFacetIds.isNotEmpty()
                )
            }

            // 搜索历史（仅在搜索框为空时显示）
            if (query.isBlank() && searchHistory.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SearchHistoryRow(
                    history = searchHistory,
                    onItemClick = { viewModel.onHistoryItemClick(it) },
                    onClearAll = { viewModel.clearSearchHistory() }
                )
            }

            Spacer(Modifier.height(8.dp))

            // 主内容区域
            when {
                query.isBlank() -> EmptyQueryState()
                isSearching -> SearchingState()
                results.isEmpty() -> NoResultsState()
                else -> ResultsList(results, navController)
            }
        }
    }
}

// ==================== 空状态、搜索中、无结果 ====================

@Composable
private fun EmptyQueryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.search_empty), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    }
}

@Composable
private fun SearchingState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.search_loading), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    }
}

@Composable
private fun NoResultsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.SearchOff, contentDescription = null, modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.search_no_results), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    }
}

// ==================== 结果列表 ====================

@Composable
private fun ResultsList(
    results: List<SearchResultItem>,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(results, key = { when (it) {
            is SearchResultItem.Device -> "dev_${it.entity.id}"
            is SearchResultItem.Layer -> "lay_${it.entity.id}"
            is SearchResultItem.Box -> "box_${it.entity.id}"
            is SearchResultItem.Sample -> "smp_${it.sample.sampleId}"
        } }) { result ->
            when (result) {
                is SearchResultItem.Device -> SearchDeviceItem(result.entity,
                    onClick = { navController.navigate(Screen.DeviceDetail.createRoute(result.entity.id)) })
                is SearchResultItem.Layer -> SearchLayerItem(result.entity, result.deviceName,
                    onClick = { navController.navigate(Screen.LayerDetail.createRoute(result.entity.id)) })
                is SearchResultItem.Box -> SearchBoxItem(result.entity, result.deviceName, result.layerName,
                    onClick = { navController.navigate(Screen.BoxGrid.createRoute(result.entity.id)) })
                is SearchResultItem.Sample -> SearchSampleItem(result.sample,
                    onClick = { navController.navigate(Screen.SampleEdit.createRoute(result.sample.sampleId)) })
            }
        }
    }
}

// ==================== Faceted 筛选区域 ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacetedFilterRow(
    facetGroups: List<FacetGroup>,
    onToggleFacet: (String) -> Unit,
    onClearAll: () -> Unit,
    hasActiveFacets: Boolean
) {
    Column {
        // 筛选标题 + 清除按钮
        if (hasActiveFacets) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearAll) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.search_facet_clear), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        facetGroups.forEach { group ->
            Text(
                text = group.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                group.options.forEach { option ->
                    FacetChip(
                        label = option.label,
                        count = option.count,
                        isSelected = option.isSelected,
                        onClick = { onToggleFacet(option.id) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FacetChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(4.dp))
            Text(
                "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ==================== 搜索历史 ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistoryRow(
    history: List<SearchHistoryItem>,
    onItemClick: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.search_history_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.search_history_clear),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClearAll)
            )
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            history.forEach { item ->
                Surface(
                    onClick = { onItemClick(item.keyword) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        text = item.keyword,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ==================== 搜索结果卡片 ====================

@Composable
private fun SearchDeviceItem(entity: StorageDeviceEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DeviceHub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(entity.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.search_device), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SearchLayerItem(entity: StorageLayerEntity, deviceName: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(entity.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$deviceName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SearchBoxItem(entity: StorageBoxEntity, deviceName: String, layerName: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(entity.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$deviceName > $layerName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SearchSampleItem(result: SampleWithPath, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(result.name ?: stringResource(R.string.fallback_unnamed), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${result.deviceName} > ${result.layerName} > ${result.boxName} > ${'A' + result.row}${result.col + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            result.note?.let { note -> if (note.isNotBlank()) { Spacer(Modifier.height(2.dp)); Text(stringResource(R.string.search_note, note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
            result.date?.let { date -> Spacer(Modifier.height(2.dp)); Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}