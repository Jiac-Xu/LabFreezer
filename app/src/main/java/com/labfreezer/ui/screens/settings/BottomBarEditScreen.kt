package com.labfreezer.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBarEditScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 从偏好读取当前可见列表
    val visibleTabs = remember { mutableStateListOf<BottomTab>().also { it.addAll(BottomTabPreference.get(context)) } }
    // 所有标签的全部列表（用于显示顺序及隐藏项）
    val allTabs = remember { BottomTab.entries }

    // 自动保存
    LaunchedEffect(visibleTabs.toList()) {
        BottomTabPreference.set(context, visibleTabs.toList())
    }

    // 拖拽状态
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("底栏编辑", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // ── 底栏预览 ──
            Text(
                "  预览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            BottomBarPreview(visibleTabs = visibleTabs)
            Spacer(Modifier.height(16.dp))

            // ── 显示项 ──
            Text(
                "  显示在底栏",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            // 可见标签列表（可拖拽排序）
            visibleTabs.forEachIndexed { index, tab ->
                val isDragged = draggedIndex == index
                TabCard(
                    tab = tab,
                    isDragged = isDragged,
                    dragOffset = dragOffset,
                    visible = true,
                    onDragStart = { draggedIndex = index; dragOffset = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val itemHeightPx = with(density) { 56.dp.toPx() }
                        dragOffset += dragAmount.y
                        val swaps = (dragOffset / itemHeightPx).roundToInt()
                        if (swaps != 0 && index + swaps in visibleTabs.indices) {
                            val newIndex = (index + swaps).coerceIn(0, visibleTabs.lastIndex)
                            val item = visibleTabs.removeAt(index)
                            visibleTabs.add(newIndex, item)
                            draggedIndex = newIndex
                            dragOffset -= swaps * itemHeightPx
                        }
                    },
                    onDragEnd = { draggedIndex = -1; dragOffset = 0f },
                    onDragCancel = { draggedIndex = -1; dragOffset = 0f },
                    onToggle = { visibleTabs.remove(tab) }
                )
            }

            if (visibleTabs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "尚未添加任何标签，请在下方选择",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 隐藏项 ──
            val hiddenTabs = allTabs.filter { it !in visibleTabs }
            if (hiddenTabs.isNotEmpty()) {
                Text(
                    "  未显示",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(8.dp))

                hiddenTabs.forEach { tab ->
                    TabCard(
                        tab = tab,
                        isDragged = false,
                        dragOffset = 0f,
                        visible = false,
                        onDragStart = {},
                        onDrag = { _, _ -> },
                        onDragEnd = {},
                        onDragCancel = {},
                        onToggle = { visibleTabs.add(tab) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 提示文字
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = "长按拖拽手柄(≡)可调整顺序，开关控制是否在底栏显示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BottomBarPreview(visibleTabs: List<BottomTab>) {
    if (visibleTabs.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(24.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleTabs.forEach { tab ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: BottomTab,
    isDragged: Boolean,
    dragOffset: Float,
    visible: Boolean,
    onDragStart: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onToggle: () -> Unit,
) {
    // 遵循 design.md SettingsCard 规范:
    //   shape = RoundedCornerShape(12.dp)
    //   elevation = 0.dp
    //   containerColor = surfaceContainerLow
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragged) dragOffset else 0f
                shadowElevation = if (isDragged) 12f else 0f
                alpha = if (visible) 1f else 0.5f
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 内部 padding 遵循 SettingsCard: horizontal=16.dp, vertical=14.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖拽手柄（仅可见标签显示）
            if (visible) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = onDragStart,
                                onDrag = onDrag,
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "拖动排序",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(12.dp))
            }

            // 图标
            Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = if (visible) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(16.dp))

            // 标签名
            Text(
                tab.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (visible) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Switch（遵循 OCR Settings 样式: checkedTrackColor = primary）
            Switch(
                checked = visible,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
