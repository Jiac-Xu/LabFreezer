package com.labfreezer.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.labfreezer.ui.glass.LiquidButton

/**
 * LabButton 颜色配置数据类，兼容 Material 3 Button 颜色语义。
 */
@Immutable
data class LabButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color = containerColor.copy(alpha = containerColor.alpha * 0.4f),
    val disabledContentColor: Color = contentColor.copy(alpha = 0.38f)
)

/**
 * LabButton 默认配置与常用变体。
 */
object LabButtonDefaults {
    val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val CompactContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    val Shape = RoundedCornerShape(12.dp)

    @Composable
    fun primaryColors(
        containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        contentColor: Color = MaterialTheme.colorScheme.onPrimary
    ) = LabButtonColors(containerColor = containerColor, contentColor = contentColor)

    @Composable
    fun surfaceColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor: Color = MaterialTheme.colorScheme.primary
    ) = LabButtonColors(containerColor = containerColor, contentColor = contentColor)

    @Composable
    fun tonalColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        contentColor: Color = MaterialTheme.colorScheme.onSurface
    ) = LabButtonColors(containerColor = containerColor, contentColor = contentColor)

    @Composable
    fun errorColors(
        containerColor: Color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        contentColor: Color = MaterialTheme.colorScheme.onErrorContainer
    ) = LabButtonColors(containerColor = containerColor, contentColor = contentColor)

    @Composable
    fun errorSolidColors(
        containerColor: Color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
        contentColor: Color = MaterialTheme.colorScheme.onError
    ) = LabButtonColors(containerColor = containerColor, contentColor = contentColor)

    @Composable
    fun buttonColors(
        containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor: Color = containerColor.copy(alpha = 0.4f),
        disabledContentColor: Color = contentColor.copy(alpha = 0.38f)
    ) = LabButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
}

/**
 * 全应用统一业务按钮（LabButton）。
 *
 * 架构层级：
 * LabFreezer UI → LabButton → LiquidButton → LocalGlassBackdrop
 *
 * - 对外完全兼容 Material 3 Button 的参数语义（onClick, modifier, enabled, shape, colors, contentPadding）；
 * - 业务页面调用方无需关心 LiquidGlass、Backdrop、Shader 等底层实现细节；
 * - 内部自动享受 LiquidButton 带来的毛玻璃磨砂（vibrancy + blur + lens）、原位弹性缩放与跟手光斑微交互；
 * - 插槽直接提供 [RowScope]，与原生 Button 一致支持直接放置 Icon、Spacer、Text。
 */
@Composable
fun LabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = LabButtonDefaults.Shape,
    colors: LabButtonColors = LabButtonDefaults.primaryColors(),
    contentPadding: PaddingValues = LabButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    LiquidButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        contentPadding = contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
