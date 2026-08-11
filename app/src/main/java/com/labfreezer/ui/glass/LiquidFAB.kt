package com.labfreezer.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * 液态玻璃 FAB（Floating Action Button）。
 *
 * 按钮处于页面内容层内部（非记录层兄弟），沿用官方对行内控件的处理：
 * 用 CanvasBackdrop 还原页面底色 + 微渐变，让玻璃本体获得磨砂/折射层次。
 * 底色为半透明（[backdropAlpha]），背后真实内容可透出，形成半透明磨砂质感。
 * 按压时缩小 + 内阴影加深，松手回弹。
 *
 * @param backdropAlpha 底色不透明度（0~1）：越低越透，0 时完全透出背后内容
 */
@Composable
fun LiquidFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    backdropColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    backdropAlpha: Float = 0.3f,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = rememberCanvasBackdrop {
        // 底色半透明：保留磨砂层次的同时让背后内容透出，上深下浅
        drawRect(backdropColor.copy(alpha = backdropAlpha))
        drawRect(
            Brush.verticalGradient(
                0f to backdropColor.copy(alpha = (backdropAlpha + 0.15f).coerceIn(0f, 1f)),
                1f to backdropColor.copy(alpha = (backdropAlpha - 0.05f).coerceIn(0f, 1f))
            )
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        label = "liquid_fab_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                    lens(6f.dp.toPx(), 8f.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.5f) },
                shadow = { Shadow(radius = 8f.dp, color = Color.Black.copy(alpha = 0.12f)) },
                innerShadow = {
                    val alpha = if (pressed) 0.25f else 0.1f
                    InnerShadow(radius = 2f.dp, alpha = alpha)
                },
                onDrawSurface = {
                    drawRect(containerColor)
                    if (pressed) {
                        drawRect(Color.Black.copy(alpha = 0.08f))
                    }
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}