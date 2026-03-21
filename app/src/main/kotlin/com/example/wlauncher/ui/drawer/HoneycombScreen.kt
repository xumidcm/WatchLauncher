package com.example.wlauncher.ui.drawer

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    narrowCols: Int = 4,
    onAppClick: (AppInfo, Offset) -> Unit,
    onLongClick: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenCenterX = screenWidthPx / 2f
        val screenCenterY = screenHeightPx / 2f
        val screenRadius = minOf(screenWidthPx, screenHeightPx) / 2f
        val useBlurApi = blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val iconSizeDp = 80.dp
        val cellSize = with(density) { iconSizeDp.toPx() }
        val iconSizePx = cellSize

        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }

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
                            val current = scrollOffset.value
                            val overscroll = when {
                                current + dragAmount.y > maxScrollY -> (current + dragAmount.y - maxScrollY)
                                current + dragAmount.y < -maxScrollY -> -((-maxScrollY) - (current + dragAmount.y))
                                else -> 0f
                            }
                            val dampedDrag = if (overscroll != 0f) dragAmount.y * 0.3f else dragAmount.y
                            scope.launch { scrollOffset.snapTo(current + dampedDrag) }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity()
                            val current = scrollOffset.value
                            if (current < -maxScrollY || current > maxScrollY) {
                                scope.launch {
                                    scrollOffset.animateTo(
                                        current.coerceIn(-maxScrollY, maxScrollY),
                                        spring(dampingRatio = 0.6f, stiffness = 400f)
                                    )
                                }
                            } else {
                                scope.launch {
                                    scrollOffset.animateDecay(velocity.y, exponentialDecay()) {
                                        if (value < -maxScrollY || value > maxScrollY) {
                                            scope.launch {
                                                scrollOffset.animateTo(
                                                    value.coerceIn(-maxScrollY, maxScrollY),
                                                    spring(dampingRatio = 0.6f, stiffness = 400f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val currentScroll = scrollOffset.value
            val visibleTop = -iconSizePx * 2
            val visibleBottom = screenHeightPx + iconSizePx * 2

            apps.forEachIndexed { index, app ->
                if (index >= positions.size) return@forEachIndexed
                val gridPos = positions[index]
                val posY = screenCenterY + gridPos.y + currentScroll

                if (posY < visibleTop || posY > visibleBottom) return@forEachIndexed

                key("${app.packageName}/${app.activityName}") {
                    AppBubble(
                        icon = app.cachedIcon,
                        size = iconSizeDp,
                        onClick = {
                            val sy = scrollOffset.value
                            val sx = screenCenterX + gridPos.x
                            val syPos = screenCenterY + gridPos.y + sy
                            onAppClick(app, Offset(sx / screenWidthPx, syPos / screenHeightPx))
                        },
                        onLongClick = {
                            vibrateHaptic(context)
                            longPressedApp = app
                        },
                        modifier = Modifier.graphicsLayer {
                            val sy = scrollOffset.value
                            val posX = screenCenterX + gridPos.x
                            val pY = screenCenterY + gridPos.y + sy

                            translationX = posX - iconSizePx / 2
                            translationY = pY - iconSizePx / 2

                            val dx = posX - screenCenterX
                            val dy = pY - screenCenterY
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val s = fisheyeScale(dist, screenRadius * 1.8f, minScale = 0.55f)
                            scaleX = s
                            scaleY = s
                            this.alpha = s.coerceIn(0.2f, 1f)

                            if (useBlurApi) {
                                val edgeDist = minOf(pY, screenHeightPx - pY)
                                val blurZone = screenHeightPx * 0.15f
                                if (edgeDist in 0f..blurZone) {
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
    }

    // App Shortcuts 弹窗
    longPressedApp?.let { app ->
        AppShortcutPopup(app = app, onDismiss = { longPressedApp = null })
    }
}
