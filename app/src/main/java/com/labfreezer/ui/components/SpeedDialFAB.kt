package com.labfreezer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.kyant.backdrop.Backdrop
import com.labfreezer.R
import com.labfreezer.ui.glass.LiquidFAB

/**
 * 全程序统一 FAB 入口。
 *
 * - 有 ≥2 个操作：展开式 Speed Dial（主按钮 + 展开选项）
 * - 只有 1 个操作：自动降级为普通液态 FAB（图标/语义可自定义）
 *
 * @param expanded 是否展开
 * @param onToggle 展开/收起回调
 * @param onCreatePrimary 主操作回调
 * @param onCreateSecond 第二个操作回调
 * @param showSecondButton 是否显示第二个按钮
 * @param secondButtonLabel 第二个按钮的文本
 * @param secondButtonIcon 第二个按钮的图标
 * @param primaryIcon 主按钮图标（降级态同样生效）
 * @param primaryButtonContentDescription 主按钮的语义描述
 */
@Composable
fun SpeedDialFAB(
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
    onCreatePrimary: () -> Unit,
    onCreateSecond: () -> Unit = {},
    showSecondButton: Boolean,
    secondButtonLabel: String = stringResource(R.string.speed_dial_create_level),
    secondButtonIcon: ImageVector = Icons.Default.Layers,
    primaryIcon: ImageVector = Icons.Default.Add,
    primaryButtonContentDescription: String = stringResource(R.string.btn_add),
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    // 只有一个操作时，降级为普通液态 FAB
    if (!showSecondButton) {
        LiquidFAB(
            onClick = onCreatePrimary,
            backdrop = backdrop,
            modifier = modifier
        ) {
            Icon(
                primaryIcon,
                contentDescription = primaryButtonContentDescription
            )
        }
        return
    }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "fab_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        // 展开的选项
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 创建盒子
                SpeedDialItem(
                    label = stringResource(R.string.speed_dial_create_box),
                    icon = {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    backdrop = backdrop,
                    onClick = {
                        onToggle()
                        onCreatePrimary()
                    }
                )

                // 第二个操作（条件显示）
                if (showSecondButton) {
                    SpeedDialItem(
                        label = secondButtonLabel,
                        icon = {
                            Icon(
                                secondButtonIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        backdrop = backdrop,
                        onClick = {
                            onToggle()
                            onCreateSecond()
                        }
                    )
                }

                // 间距放 AnimatedVisibility 内部，收起时一并隐藏，避免闪烁
                Spacer(Modifier.height(12.dp))
            }
        }

        // 主 FAB 按钮
        LiquidFAB(
            onClick = onToggle,
            backdrop = backdrop
        ) {
            Icon(
                primaryIcon,
                contentDescription = if (expanded)
                    stringResource(R.string.content_description_collapse)
                else
                    primaryButtonContentDescription,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

/**
 * Speed Dial 中的单个选项项。
 */
@Composable
private fun SpeedDialItem(
    label: String,
    icon: @Composable () -> Unit,
    backdrop: Backdrop? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        // 文字标签
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // 图标按钮
        LiquidFAB(
            onClick = onClick,
            size = 40.dp,
            backdrop = backdrop,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            icon()
        }
    }
}
