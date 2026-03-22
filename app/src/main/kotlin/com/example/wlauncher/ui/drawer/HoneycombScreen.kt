package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.anim.platformBlur
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    narrowCols: Int = 4,
    topBlurRadiusDp: Int = 12,
    bottomBlurRadiusDp: Int = 12,
    topFadeRangeDp: Int = 56,
    bottomFadeRangeDp: Int = 56,
    onAppClick: (AppInfo, Offset) -> Unit,
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
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

        val iconSizeDp = 80.dp
        val cellSize = with(density) { iconSizeDp.toPx() }
        val iconSizePx = cellSize
        val topFadePx = with(density) { topFadeRangeDp.dp.toPx() }
        val bottomFadePx = with(density) { bottomFadeRangeDp.dp.toPx() }

        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }

        val safeTop = topFadePx + iconSizePx * 0.55f
        val safeBottom = screenHeightPx - bottomFadePx - iconSizePx * 0.55f
        val minGridY = positions.minOfOrNull { it.y } ?: 0f
        val maxGridY = positions.maxOfOrNull { it.y } ?: 0f
        val maxScroll = safeTop - (screenCenterY + minGridY)
        val minScroll = safeBottom - (screenCenterY + maxGridY)

        val scrollOffset = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val overlayBlurActive = longPressedApp != null && blurEnabled

        Box(
            modifier = Modifier
                .fillMaxSize()
                .platformBlur(16f, overlayBlurActive)
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
                            val next = current + dragAmount.y
                            val overscroll = when {
                                next > maxScroll -> next - maxScroll
                                next < minScroll -> next - minScroll
                                else -> 0f
                            }
                            val dampedDrag = if (overscroll != 0f) dragAmount.y * 0.3f else dragAmount.y
                            scope.launch { scrollOffset.snapTo(current + dampedDrag) }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().y
                            val current = scrollOffset.value
                            if (current >= maxScroll - cellSize * 0.5f && velocity > 800f) {
                                onScrollToTop()
                                return@detectDragGestures
                            }
                            if (current < minScroll || current > maxScroll) {
                                scope.launch {
                                    scrollOffset.animateTo(
                                        current.coerceIn(minScroll, maxScroll),
                                        spring(dampingRatio = 0.6f, stiffness = 400f)
                                    )
                                }
                            } else {
                                scope.launch {
                                    scrollOffset.animateDecay(velocity, exponentialDecay()) {
                                        if (value < minScroll || value > maxScroll) {
                                            scope.launch {
                                                scrollOffset.animateTo(
                                                    value.coerceIn(minScroll, maxScroll),
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
            val visibleTop = -iconSizePx * 1.5f
            val visibleBottom = screenHeightPx + iconSizePx * 1.5f

            apps.forEachIndexed { index, app ->
                if (index >= positions.size) return@forEachIndexed
                val gridPos = positions[index]
                val posY = screenCenterY + gridPos.y + currentScroll
                if (posY < visibleTop || posY > visibleBottom) return@forEachIndexed

                val topStrength = edgeStrength(
                    position = posY,
                    leadingRange = topFadePx
                )
                val bottomStrength = edgeStrength(
                    position = screenHeightPx - posY,
                    leadingRange = bottomFadePx
                )
                val topBlur = topStrength * topBlurRadiusDp
                val bottomBlur = bottomStrength * bottomBlurRadiusDp
                val itemBlur = maxOf(topBlur, bottomBlur)

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
                            onLongClick(app)
                            longPressedApp = app
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                val sy = scrollOffset.value
                                val posX = screenCenterX + gridPos.x
                                val pY = screenCenterY + gridPos.y + sy
                                translationX = posX - iconSizePx / 2
                                translationY = pY - iconSizePx / 2

                                val dx = posX - screenCenterX
                                val dy = pY - screenCenterY
                                val dist = sqrt(dx * dx + dy * dy)
                                val scale = fisheyeScale(dist, screenRadius * 1.8f, minScale = 0.55f)
                                scaleX = scale
                                scaleY = scale
                                alpha = scale.coerceIn(0.2f, 1f)
                            }
                            .platformBlur(itemBlur, blurEnabled && edgeBlurEnabled)
                    )
                }
            }
        }

        if (topFadeRangeDp > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topFadeRangeDp.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
            )
        }
        if (bottomFadeRangeDp > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomFadeRangeDp.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            )
        }
    }

    longPressedApp?.let { app ->
        AppShortcutOverlay(
            app = app,
            blurEnabled = blurEnabled,
            onDismiss = { longPressedApp = null }
        )
    }
}

private fun edgeStrength(
    position: Float,
    leadingRange: Float
): Float {
    if (leadingRange <= 0f) return 0f
    if (position <= 0f || position >= leadingRange) return 0f
    return (1f - (position / leadingRange)).coerceIn(0f, 1f)
}
