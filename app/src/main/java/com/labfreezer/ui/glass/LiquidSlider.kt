package com.labfreezer.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest

/**
 * 移植自 Kyant0/AndroidLiquidGlass catalog（com.kyant.backdrop.catalog.components.LiquidSlider）
 * 统一模糊接口：优先从显式传入的 [backdrop] 或 [LocalGlassBackdrop] 获取真实采样层，无真实采样层时自动退化为画布底色。
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    snapPoints: List<Float> = emptyList(),
    accentColor: Color = Color(0xFF0088FF),
    trackColor: Color = Color(0xFF787878).copy(alpha = 0.2f),
    backdropColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    backdropAlpha: Float = 0.3f
) {
    val canvasBackdrop = rememberCanvasBackdrop {
        drawRect(backdropColor.copy(alpha = backdropAlpha))
        drawRect(
            Brush.verticalGradient(
                0f to backdropColor.copy(alpha = (backdropAlpha + 0.15f).coerceIn(0f, 1f)),
                1f to backdropColor.copy(alpha = (backdropAlpha - 0.05f).coerceIn(0f, 1f))
            )
        )
    }
    val effectiveBackdrop = backdrop ?: LocalGlassBackdrop.current ?: canvasBackdrop
    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope, valueRange, snapPoints) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val rangeSpan = valueRange.endInclusive - valueRange.start
                    if (rangeSpan > 0f) {
                        val delta = rangeSpan * (dragAmount.x / trackWidth)
                        val rawValue =
                            if (isLtr) (targetValue + delta).coerceIn(valueRange)
                            else (targetValue - delta).coerceIn(valueRange)
                        onValueChange(rawValue)
                    }
                }
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { value ->
                    if (dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .pointerInput(animationScope, valueRange, isLtr) {
                        detectTapGestures { position ->
                            val rangeSpan = valueRange.endInclusive - valueRange.start
                            if (rangeSpan > 0f) {
                                val delta = rangeSpan * (position.x / trackWidth)
                                val rawValue =
                                    (if (isLtr) valueRange.start + delta
                                    else valueRange.endInclusive - delta)
                                        .coerceIn(valueRange)
                                dampedDragAnimation.animateToValue(rawValue)
                                onValueChange(rawValue)
                            }
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )

            // 吸附点指示圆点
            snapPoints.forEach { snapValue ->
                if (snapValue in valueRange && (valueRange.endInclusive - valueRange.start) > 0f) {
                    val snapProgress = (snapValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                    val isPassed = dampedDragAnimation.progress >= snapProgress
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer {
                                val x = trackWidth * (if (isLtr) snapProgress else 1f - snapProgress)
                                translationX = (x - size.width / 2f).fastCoerceIn(0f, trackWidth - size.width)
                            }
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPassed) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }

        // 吸附点触摸扩展区域（点击即可精确切换至吸附值）
        snapPoints.forEach { snapValue ->
            if (snapValue in valueRange && (valueRange.endInclusive - valueRange.start) > 0f) {
                val snapProgress = (snapValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            val x = trackWidth * (if (isLtr) snapProgress else 1f - snapProgress)
                            translationX = (x - size.width / 2f).fastCoerceIn(0f, trackWidth - size.width)
                        }
                        .size(40.dp, 32.dp)
                        .pointerInput(snapValue, animationScope) {
                            detectTapGestures {
                                dampedDragAnimation.animateToValue(snapValue)
                                onValueChange(snapValue)
                            }
                        }
                )
            }
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        effectiveBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}