package com.labfreezer.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * 玻璃采样层（CompositionLocal）。
 * 由 [com.labfreezer.ui.components.GlassScaffold]（屏幕内容层）或 MainScreen（全局内容层，
 * 供悬浮底栏）提供：把内容记录成真实图层，悬浮玻璃组件作为兄弟节点采样该层即可磨砂出真实内容。
 * 注意：页面内部的普通内联组件绝不能向上采样包裹自己的父图层，否则会导致 GPU 循环递归崩溃。
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 液态玻璃通用按钮（操作按钮、文字标签按钮等）。
 *
 * 走与 [LiquidFAB] 完全统一的模糊与光影规范：
 * - 统一的磨砂/折射（vibrancy + blur 6dp + lens 6dp/8dp）与光影层次（高光 + 阴影 + 按下内阴影）；
 * - 统一的跟手光斑（[InteractiveHighlight]）与原位弹性弹簧按压缩放；
 * - 默认使用独立的合成画布底色（[rememberCanvasBackdrop]），绝不向外递归采样父容器自身，确保 100% 稳定防闪退；
 * - 仅当外部显式传入 [backdrop]（如悬浮 FAB 采样同级内容层）时才进行跨层采样。
 *
 * @param onClick 点击回调
 * @param modifier 外部修饰符
 * @param enabled 是否可用，为 false 时禁用点击并呈现半透明禁用态
 * @param shape 形状，默认 12dp 圆角
 * @param containerColor 表面底色（建议半透明）
 * @param contentColor 内容文字/图标颜色
 * @param backdropColor 无真实采样层时的退化背景基色
 * @param backdropAlpha 无真实采样层时的退化背景不透明度
 * @param backdrop 可选的真实内容层（LayerBackdrop）。传入后对真实内容采样磨砂；不传则使用安全的局部画布合成层
 * @param contentPadding 按钮内部边距
 * @param content 按钮内容
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    backdropColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    backdropAlpha: Float = 0.3f,
    backdrop: Backdrop? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit
) {
    // 禁用态色彩适配
    val effectiveContainerColor = if (enabled) {
        containerColor
    } else {
        containerColor.copy(alpha = containerColor.alpha * 0.4f)
    }
    val effectiveContentColor = if (enabled) {
        contentColor
    } else {
        contentColor.copy(alpha = 0.38f)
    }

    // 局部合成画布底色（半透明纯色 + 微渐变）
    val canvasBackdrop = rememberCanvasBackdrop {
        drawRect(backdropColor.copy(alpha = if (enabled) backdropAlpha else backdropAlpha * 0.5f))
        drawRect(
            Brush.verticalGradient(
                0f to backdropColor.copy(alpha = if (enabled) (backdropAlpha + 0.15f).coerceIn(0f, 1f) else (backdropAlpha * 0.5f)),
                1f to backdropColor.copy(alpha = if (enabled) (backdropAlpha - 0.05f).coerceIn(0f, 1f) else 0f)
            )
        )
    }
    // 关键防闪退设计：仅当显式传入 backdrop 时采用外部层；内联按钮默认使用自身合成画布层，绝不向外采样父容器
    val effectiveBackdrop = backdrop ?: canvasBackdrop

    val interactionSource = remember { MutableInteractionSource() }
    val rawPressed by interactionSource.collectIsPressedAsState()
    val pressed = enabled && rawPressed

    // 跟手光影：手指触按处产生白色光斑，弹簧动画随手指移动
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    // 弹簧缩放：按下时回弹带轻微过冲，原位弹性缩放
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(0.5f, 300f),
        label = "liquid_button_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBackdrop(
                backdrop = effectiveBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                    lens(6f.dp.toPx(), 8f.dp.toPx())
                },
                highlight = { if (enabled) Highlight.Default.copy(alpha = 0.5f) else Highlight.Default.copy(alpha = 0.1f) },
                shadow = { Shadow(radius = 8f.dp, color = Color.Black.copy(alpha = if (enabled) 0.12f else 0.04f)) },
                innerShadow = {
                    val alpha = if (pressed) 0.25f else if (enabled) 0.1f else 0f
                    InnerShadow(radius = 2f.dp, alpha = alpha)
                },
                onDrawSurface = {
                    drawRect(effectiveContainerColor)
                    if (pressed) {
                        drawRect(Color.Black.copy(alpha = 0.08f))
                    }
                }
            )
            .then(if (enabled) interactiveHighlight.modifier else Modifier)
            .then(if (enabled) interactiveHighlight.gestureModifier else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides effectiveContentColor) {
            content()
        }
    }
}
