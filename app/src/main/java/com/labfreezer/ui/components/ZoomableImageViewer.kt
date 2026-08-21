package com.labfreezer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.labfreezer.R
import com.labfreezer.ui.glass.LiquidFAB
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/** 最大缩放倍数 */
private const val MAX_SCALE = 5f
/** 双击放大时的目标倍数 */
private const val DOUBLE_TAP_SCALE = 3f

/**
 * 全屏图片查看 Dialog：双指缩放（1x~5x）、缩放时单指平移、双击放大/还原、单击关闭。
 * 平移会被限制在图片边界内，图片边缘不会离开屏幕。
 *
 * @param onSave 非空时在右下角显示液态玻璃「保存」按钮，点击回调（如保存到相册）
 */
@Composable
fun ZoomableImageViewer(
    model: Any?,
    onDismiss: () -> Unit,
    contentDescription: String? = null,
    onSave: (() -> Unit)? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // 屏幕（视口）尺寸
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    // 图片原始尺寸（加载成功后从 painter 获取）
    var contentSize by remember { mutableStateOf(Size.Unspecified) }
    // 图片以 Fit 缩放后的渲染尺寸
    var fittedSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(contentSize, viewportSize) {
        if (contentSize != Size.Unspecified && viewportSize.width > 0 && viewportSize.height > 0) {
            val ratio = min(
                viewportSize.width / contentSize.width,
                viewportSize.height / contentSize.height
            )
            fittedSize = Size(contentSize.width * ratio, contentSize.height * ratio)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { viewportSize = it }
                .pointerInput(Unit) {
                    coroutineScope {
                        // 单击关闭 / 双击缩放；与下方变换手势并行运行，互不阻塞
                        launch {
                            detectTapGestures(
                                onTap = { onDismiss() },
                                onDoubleTap = { tapPos ->
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        // 以双击点为中心放大：保持该点对应的画面内容位置不变
                                        scale = DOUBLE_TAP_SCALE
                                        offset = clampOffset(
                                            tapPos - tapPos * DOUBLE_TAP_SCALE,
                                            scale, fittedSize, viewportSize
                                        )
                                    }
                                }
                            )
                        }
                        // 双指缩放 + 单指平移
                        launch {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                                offset = clampOffset(offset + pan, scale, fittedSize, viewportSize)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val s = state.painter.intrinsicSize
                    if (s != Size.Unspecified && s.width > 0f && s.height > 0f) {
                        contentSize = s
                    }
                }
            )

            // 右下角保存按钮（液态玻璃），fixed 尺寸外层优先，构成胶囊形按钮
            onSave?.let { save ->
                LiquidFAB(
                    onClick = save,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .width(108.dp)
                        .height(48.dp),
                    backdropColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.btn_save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * 将平移量限制在图片边界内：缩放后图片边缘不会离开屏幕。
 * 图片未加载完成、渲染尺寸未知时以视口尺寸兜底。
 */
private fun clampOffset(
    offset: Offset,
    scale: Float,
    fittedSize: Size,
    viewportSize: IntSize
): Offset {
    if (scale <= 1f) return Offset.Zero
    val baseW = if (fittedSize.width > 0f) fittedSize.width else viewportSize.width.toFloat()
    val baseH = if (fittedSize.height > 0f) fittedSize.height else viewportSize.height.toFloat()
    val maxX = max(0f, (baseW * scale - viewportSize.width) / 2f)
    val maxY = max(0f, (baseH * scale - viewportSize.height) / 2f)
    return Offset(
        offset.x.coerceIn(-maxX, maxX),
        offset.y.coerceIn(-maxY, maxY)
    )
}