package com.labfreezer.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * 液态玻璃 FAB（Floating Action Button）。
 *
 * 按钮处于页面内容层内部（非记录层兄弟），沿用官方对行内控件的处理：
 * 用 CanvasBackdrop 还原页面底色 + 微渐变，让玻璃本体获得磨砂/折射层次。
 * 底色为半透明（[backdropAlpha]），背后真实内容可透出，形成半透明磨砂质感。
 * 触按时：跟手白色光斑（InteractiveHighlight，移植自 AndroidLiquidGlass）+ 弹簧缩放回弹 +
 * 轻微拖动移位（跟随手指，EaseOut 钳制在 [maxDragDisplacement]，松手弹簧归位）。
 *
 * @param backdropAlpha 底色不透明度（0~1）：越低越透，0 时完全透出背后内容
 * @param maxDragDisplacement 拖动位移上限
 * @param backdrop 可选的真实内容层（LayerBackdrop）。传入后玻璃本体对真实内容采样磨砂，
 *   与底栏效果一致；不传则退化为画布底色（半透明纯色 + 渐变）
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
    maxDragDisplacement: Dp = 4.dp,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // 无真实层时退化为画布底色（半透明纯色 + 微渐变）
    val canvasBackdrop = rememberCanvasBackdrop {
        // 底色半透明：保留磨砂层次的同时让背后内容透出，上深下浅
        drawRect(backdropColor.copy(alpha = backdropAlpha))
        drawRect(
            Brush.verticalGradient(
                0f to backdropColor.copy(alpha = (backdropAlpha + 0.15f).coerceIn(0f, 1f)),
                1f to backdropColor.copy(alpha = (backdropAlpha - 0.05f).coerceIn(0f, 1f))
            )
        )
    }
    val effectiveBackdrop = backdrop ?: canvasBackdrop

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // 跟手光影：手指触按处产生白色光斑，弹簧动画随手指移动
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    // 弹簧缩放：与高光同款阻尼/劲度，按下回弹带轻微过冲
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(0.5f, 300f),
        label = "liquid_fab_scale"
    )

    // 拖动移位：累积手指拖动量，EaseOut 钳制在 maxDragDisplacement 内（同底栏 panelOffset 逻辑）
    val density = LocalDensity.current
    val maxDisplacementPx = with(density) { maxDragDisplacement.toPx() }
    val maxDragPx = with(density) { 120.dp.toPx() }
    val dragOffsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold) }
    val dragDisplacement by remember(density) {
        derivedStateOf {
            Offset(
                maxDisplacementPx * sign(dragOffsetAnimation.value.x) *
                    EaseOut.transform(abs(dragOffsetAnimation.value.x / maxDragPx).fastCoerceIn(0f, 1f)),
                maxDisplacementPx * sign(dragOffsetAnimation.value.y) *
                    EaseOut.transform(abs(dragOffsetAnimation.value.y / maxDragPx).fastCoerceIn(0f, 1f))
            )
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationX = dragDisplacement.x
                translationY = dragDisplacement.y
            }
            .drawBackdrop(
                backdrop = effectiveBackdrop,
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
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .pointerInput(animationScope) {
                // 与光斑检测并行：只跟踪拖动量，不消费事件（同底栏双检测器模式）
                inspectDragGestures(
                    onDragStart = {
                        animationScope.launch { dragOffsetAnimation.snapTo(Offset.Zero) }
                    },
                    onDragEnd = {
                        animationScope.launch {
                            dragOffsetAnimation.animateTo(Offset.Zero, spring(1f, 300f, Offset.VisibilityThreshold))
                        }
                    },
                    onDragCancel = {
                        animationScope.launch {
                            dragOffsetAnimation.animateTo(Offset.Zero, spring(1f, 300f, Offset.VisibilityThreshold))
                        }
                    }
                ) { _, dragAmount ->
                    animationScope.launch {
                        dragOffsetAnimation.snapTo(dragOffsetAnimation.value + dragAmount)
                    }
                }
            }
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