package com.labfreezer.ui.screens.tags
import com.labfreezer.R

import com.labfreezer.data.model.Position

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.ui.navigation.Screen
import com.labfreezer.ui.screens.sample.BrowseContextStore
import com.labfreezer.ui.screens.sample.SampleBrowseContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(navController: NavController, tagId: Long, viewModel: TagDetailViewModel = hiltViewModel()) {
    val tag by viewModel.tag.collectAsStateWithLifecycle()
    val samples by viewModel.samples.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag?.name ?: stringResource(R.string.tag_detail_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (samples.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tag_detail_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + padding.calculateBottomPadding())
            ) {
                items(samples, key = { it.sampleId }) { result ->
                    val sampleIds = samples.map { it.sampleId }
                    val tagCtx = SampleBrowseContext.Tag(
                        tagId = tagId,
                        tagName = tag?.name ?: "",
                        sampleIds = sampleIds
                    )
                    val ctxKey = remember(tagId, tag?.name, sampleIds.size) { BrowseContextStore.put(tagCtx) }
                    SampleItem(result = result, onClick = {
                        navController.navigate(Screen.SampleEdit.createRoute(result.sampleId, ctxKey))
                    })
                }
            }
        }
    }
}

@Composable
private fun SampleItem(result: SampleWithPath, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(result.name ?: stringResource(R.string.fallback_unnamed), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    if (result.deviceName.isNotBlank()) append(result.deviceName)
                    if (result.layerName.isNotBlank()) {
                        if (result.deviceName.isNotBlank()) append(" > ")
                        append(result.layerName)
                    }
                    append(" > ${result.boxName} > ${Position.toLabel(result.row, result.col)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
