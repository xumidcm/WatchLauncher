package com.example.wlauncher.ui.drawer

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.theme.WatchColors
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.launch
import kotlin.math.abs
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
        val screenCenterX = screenWidthPx / 2f
        val screenCenterY = screenHeightPx / 2f
        val screenRadius = minOf(screenWidthPx, screenHeightPx) / 2f

        val iconSizeDp = 80.dp
        val cellSize = with(density) { iconSizeDp.toPx() }
        val iconSizePx = cellSize
        val narrowCols = 4

        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }

        // 用 Animatable 驱动滚动，避免 state 触发重组
        val scrollOffset = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        val maxScrollY = remember(positions, cellSize) {
            if (positions.isEmpty()) 0f
            else positions.maxOf { abs(it.y) } + cellSize
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { scrollOffset.stop() }
                            velocityTracker.resetTracking()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newVal = (scrollOffset.value + dragAmount.y)
                                .coerceIn(-maxScrollY, maxScrollY)
                            scope.launch { scrollOffset.snapTo(newVal) }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            scope.launch {
                                scrollOffset.animateDecay(velocity.y, exponentialDecay()) {
                                    // clamp during decay
                                    if (value < -maxScrollY || value > maxScrollY) {
                                        scope.launch {
                                            scrollOffset.snapTo(value.coerceIn(-maxScrollY, maxScrollY))
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            apps.forEachIndexed { index, app ->
                if (index >= positions.size) return@forEachIndexed
                val gridPos = positions[index]

                // 用 key 确保 composable 稳定
                key(app.packageName) {
                    AppBubble(
                        icon = app.cachedIcon,
                        size = iconSizeDp,
                        onClick = {
                            val sy = scrollOffset.value
                            val sx = screenCenterX + gridPos.x
                            val syPos = screenCenterY + gridPos.y + sy
                            onAppClick(app, Offset(sx / screenWidthPx, syPos / screenHeightPx))
                        },
                        modifier = Modifier.graphicsLayer {
                            val sy = scrollOffset.value
                            val posX = screenCenterX + gridPos.x
                            val posY = screenCenterY + gridPos.y + sy

                            // 视口裁剪
                            if (posY < -iconSizePx || posY > screenHeightPx + iconSizePx) {
                                alpha = 0f
                                return@graphicsLayer
                            }

                            translationX = posX - iconSizePx / 2
                            translationY = posY - iconSizePx / 2

                            // 鱼眼缩放
                            val dx = posX - screenCenterX
                            val dy = posY - screenCenterY
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val s = fisheyeScale(dist, screenRadius * 1.8f, minScale = 0.55f)
                            scaleX = s
                            scaleY = s
                            this.alpha = s.coerceIn(0.2f, 1f)

                            // 边缘模糊 (API 31+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val edgeDist = minOf(posY, screenHeightPx - posY)
                                val blurZone = screenHeightPx * 0.15f
                                if (edgeDist < blurZone && edgeDist > 0) {
                                    val blurAmount = ((1f - edgeDist / blurZone) * 12f).coerceIn(0f, 12f)
                                    if (blurAmount > 0.5f) {
                                        renderEffect = RenderEffect.createBlurEffect(
                                            blurAmount * density.density,
                                            blurAmount * density.density,
                                            Shader.TileMode.CLAMP
                                        ).asComposeRenderEffect()
                                    }
                                }
                            }
                        }
                    )
                }
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
