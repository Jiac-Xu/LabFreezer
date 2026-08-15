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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * 玻璃采样层（CompositionLocal）。
 * 由 [com.labfreezer.ui.components.GlassScaffold]（屏幕内容层）或 MainScreen（全局内容层，
 * 供悬浮底栏）提供：把内容记录成真实图层，玻璃组件作为兄弟节点采样该层即可磨砂出真实内容。
 * 不提供时液态玻璃组件自动退化为画布底色。
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 液态玻璃通用按钮（文字按钮、图文按钮等）。
 *
 * 走与 [LiquidFAB] 完全统一的模糊与光影规范：
 * - 优先采用 [LocalGlassBackdrop] 或显式传入的 [backdrop] 采样真实图层；无真实层时退化为半透明渐变画布底色；
 * - 统一的磨砂/折射（vibrancy + blur 6dp + lens 6dp/8dp）与光影层次（高光 + 阴影 + 按下内阴影）；
 * - 统一的跟手光斑（[InteractiveHighlight]）、弹簧按压缩放与微位移拖拽手势。
 *
 * @param onClick 点击回调
 * @param modifier 外部修饰符
 * @param shape 形状，默认 12dp 圆角
 * @param containerColor 表面底色（建议半透明）
 * @param contentColor 内容文字/图标颜色
 * @param backdropColor 无真实采样层时的退化背景基色
 * @param backdropAlpha 无真实采样层时的退化背景不透明度
 * @param maxDragDisplacement 拖动位移上限
 * @param backdrop 可选的真实内容层（LayerBackdrop）。传入后对真实内容采样磨砂；不传则从 [LocalGlassBackdrop] 获取或退化为画布底色
 * @param contentPadding 按钮内部边距
 * @param content 按钮内容
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    backdropColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    backdropAlpha: Float = 0.3f,
    maxDragDisplacement: Dp = 4.dp,
    backdrop: Backdrop? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
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
    // 优先显式传入的层，其次由 GlassScaffold / MainScreen 提供的真实内容层，最后退化画布底色
    val effectiveBackdrop = backdrop ?: LocalGlassBackdrop.current ?: canvasBackdrop

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // 跟手光影：手指触按处产生白色光斑，弹簧动画随手指移动
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    // 弹簧缩放：与高光同款阻尼/劲度，按下回弹带轻微过冲
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(0.5f, 300f),
        label = "liquid_button_scale"
    )

    // 拖动移位：累积手指拖动量，EaseOut 钳制在 maxDragDisplacement 内
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
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationX = dragDisplacement.x
                translationY = dragDisplacement.y
            }
            .drawBackdrop(
                backdrop = effectiveBackdrop,
                shape = { shape },
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
                // 与光斑检测并行：只跟踪拖动量，不消费事件
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
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
