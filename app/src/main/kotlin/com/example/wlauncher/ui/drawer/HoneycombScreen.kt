package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHexSpiral
import com.example.wlauncher.util.hexToPixel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    onAppClick: (AppInfo, Offset) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenCenter = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
        val screenRadius = minOf(screenWidthPx, screenHeightPx) / 2f

        // 六边形网格参数
        val cellSize = with(density) { 38.dp.toPx() }
        val iconSizeDp = 54.dp

        val hexPositions = remember(apps.size) { generateHexSpiral(apps.size) }

        // 平移状态 + 惯性
        var panOffset by remember { mutableStateOf(Offset.Zero) }
        val scope = rememberCoroutineScope()
        val animX = remember { Animatable(0f) }
        val animY = remember { Animatable(0f) }
        val velocityTracker = remember { VelocityTracker() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { animX.stop() }
                            scope.launch { animY.stop() }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            panOffset += dragAmount
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            // 惯性滑动
                            scope.launch {
                                animX.snapTo(panOffset.x)
                                animX.animateDecay(velocity.x, exponentialDecay())
                            }
                            scope.launch {
                                animY.snapTo(panOffset.y)
                                animY.animateDecay(velocity.y, exponentialDecay())
                            }
                        }
                    )
                }
        ) {
            // 更新 panOffset（惯性动画时）
            LaunchedEffect(Unit) {
                snapshotFlow { Offset(animX.value, animY.value) }
                    .collect { panOffset = it }
            }

            apps.forEachIndexed { index, app ->
                if (index >= hexPositions.size) return@forEachIndexed

                val hexPixel = hexToPixel(hexPositions[index], cellSize)
                val worldPos = hexPixel + panOffset
                val screenPos = screenCenter + worldPos
                val distFromCenter = worldPos.getDistance()

                // 视口裁剪：跳过屏幕外的图标
                if (distFromCenter > screenRadius + cellSize * 3) return@forEachIndexed

                val scale = fisheyeScale(distFromCenter, screenRadius)
                val iconSizePx = with(density) { iconSizeDp.toPx() }

                AppBubble(
                    icon = app.icon,
                    size = iconSizeDp,
                    onClick = {
                        val normalizedOrigin = Offset(
                            screenPos.x / screenWidthPx,
                            screenPos.y / screenHeightPx
                        )
                        onAppClick(app, normalizedOrigin)
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (screenPos.x - iconSizePx / 2).roundToInt(),
                                (screenPos.y - iconSizePx / 2).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = scale.coerceIn(0.3f, 1f)
                        }
                )
            }
        }
    }
}
