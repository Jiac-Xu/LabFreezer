package com.labfreezer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.labfreezer.ui.glass.LocalGlassBackdrop

/**
 * 支持真实玻璃磨砂的 Scaffold。
 *
 * 与 [Scaffold] 完全同参，额外把屏幕内容记录成真实图层，并通过 [LocalGlassBackdrop]
 * 提供给其中的 LiquidFAB 采样——FAB 会自动磨砂出背后的真实内容（与底栏效果一致），
 * 无需在调用方逐个传 backdrop。
 *
 * FAB 位于 Scaffold 的 floatingActionButton 槽中，是内容层的兄弟节点，可安全采样不自我引用。
 */
@Composable
fun GlassFabScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val fabBackdrop = rememberLayerBackdrop()

    CompositionLocalProvider(LocalGlassBackdrop provides fabBackdrop) {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
        ) { padding ->
            // 记录屏幕内容层，供 FAB 玻璃采样磨砂
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(fabBackdrop)) {
                content(padding)
            }
        }
    }
}
