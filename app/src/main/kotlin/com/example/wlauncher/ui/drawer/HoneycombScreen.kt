package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.theme.WatchColors
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

        // 图标尺寸和列数（固定 4-5 交替，匹配 Apple Watch 布局）
        val iconSizeDp = 80.dp
        val cellSize = with(density) { iconSizeDp.toPx() }
        val narrowCols = 4

        // 行列式蜂窝布局
        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }

        // Y 轴滚动
        var panOffsetY by remember { mutableFloatStateOf(0f) }
        val scope = rememberCoroutineScope()
        val animY = remember { Animatable(0f) }

        // 滚动边界
        val maxScrollY = remember(positions) {
            if (positions.isEmpty()) 0f
            else positions.maxOf { kotlin.math.abs(it.y) } + cellSize
        }

        fun clampY(y: Float): Float = y.coerceIn(-maxScrollY, maxScrollY)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { animY.stop() }
                            velocityTracker.resetTracking()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                            panOffsetY = clampY(panOffsetY + dragAmount.y)
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            scope.launch {
                                animY.snapTo(panOffsetY)
                                animY.animateDecay(velocity.y, exponentialDecay())
                            }
                        }
                    )
                }
        ) {
            LaunchedEffect(Unit) {
                snapshotFlow { animY.value }
                    .collect { panOffsetY = clampY(it) }
            }

            val iconSizePx = with(density) { iconSizeDp.toPx() }

            apps.forEachIndexed { index, app ->
                if (index >= positions.size) return@forEachIndexed

                val gridPos = positions[index]
                val screenPos = Offset(
                    screenCenter.x + gridPos.x,
                    screenCenter.y + gridPos.y + panOffsetY
                )

                // 视口裁剪
                if (screenPos.y < -iconSizePx || screenPos.y > screenHeightPx + iconSizePx
                ) return@forEachIndexed

                // 鱼眼：基于到屏幕中心的距离
                val distFromCenter = Offset(
                    screenPos.x - screenCenter.x,
                    screenPos.y - screenCenter.y
                ).getDistance()
                val scale = fisheyeScale(distFromCenter, screenRadius * 1.8f, minScale = 0.55f)

                AppBubble(
                    icon = app.cachedIcon,
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
                            alpha = scale.coerceIn(0.2f, 1f)
                        }
                )
            }
        }

        // 顶部渐变遮罩
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
        )
        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )

        // 设置按钮
        FloatingActionButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .size(40.dp),
            shape = CircleShape,
            containerColor = WatchColors.SurfaceGlass,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "设置",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
