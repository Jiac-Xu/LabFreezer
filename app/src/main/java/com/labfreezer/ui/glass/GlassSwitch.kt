package com.labfreezer.ui.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

/**
 * 设置卡片内的液态玻璃开关。
 *
 * 卡片内开关无法把整页内容包进 layerBackdrop（玻璃层必须是记录层的兄弟节点），
 * 沿用官方 demo 对行内开关的处理：用 CanvasBackdrop 还原卡片底色 + 微渐变，
 * 让玻璃旋钮获得折射层次。禁用态回落到原生 Switch。
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    backdropColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    if (!enabled) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = trackColor
            )
        )
        return
    }
    val backdrop = rememberCanvasBackdrop {
        drawRect(backdropColor)
        drawRect(
            Brush.verticalGradient(
                0f to backdropColor.copy(alpha = 0.95f),
                1f to backdropColor.copy(alpha = 0.8f)
            )
        )
    }
    LiquidToggle(
        selected = { checked },
        onSelect = onCheckedChange,
        backdrop = backdrop,
        modifier = modifier,
        accentColor = accentColor,
        trackColor = trackColor
    )
}