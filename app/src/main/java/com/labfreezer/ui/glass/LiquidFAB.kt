package com.labfreezer.ui.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule

/**
 * 液态玻璃 FAB（Floating Action Button）。
 *
 * 基于 [LiquidButton] 构建，固定圆形/胶囊形与指定尺寸。
 * FAB 位于 Scaffold 的 [androidx.compose.material3.Scaffold] 浮动动作按钮槽位（在页面内容层外部），
 * 默认自动从 [LocalGlassBackdrop] 采样底下的内容图层实现真实毛玻璃磨砂。
 *
 * @param backdropAlpha 底色不透明度（0~1）：越低越透，0 时完全透出背后内容
 * @param backdrop 可选的真实内容层（LayerBackdrop）。传入后玻璃本体对真实内容采样磨砂；
 *   不传则自动从 [LocalGlassBackdrop] 获取或退化为画布底色
 */
@Composable
fun LiquidFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    backdropColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    backdropAlpha: Float = 0.3f,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveBackdrop = backdrop ?: LocalGlassBackdrop.current
    LiquidButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        shape = Capsule(),
        containerColor = containerColor,
        contentColor = contentColor,
        backdropColor = backdropColor,
        backdropAlpha = backdropAlpha,
        backdrop = effectiveBackdrop,
        contentPadding = PaddingValues(0.dp),
        content = content
    )
}