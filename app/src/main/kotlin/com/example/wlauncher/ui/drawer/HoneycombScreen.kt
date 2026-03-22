package com.example.wlauncher.ui.drawer

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    suppressHeavyEffects: Boolean = false,
    narrowCols: Int = 4,
    topBlurRadiusDp: Int = 12,
    bottomBlurRadiusDp: Int = 12,
    topFadeRangeDp: Int = 56,
    bottomFadeRangeDp: Int = 56,
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }
    var pressedAppKey by remember { mutableStateOf<String?>(null) }
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val effectiveEdgeBlur = edgeBlurEnabled && !suppressHeavyEffects

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenCenterX = screenWidthPx / 2f
        val screenCenterY = screenHeightPx / 2f
        val screenRadius = minOf(screenWidthPx, screenHeightPx) / 2f

        val maxCols = narrowCols + 1
        val availableWidth = screenWidthPx - with(density) { 20.dp.toPx() }
        val iconSizePx = (availableWidth / (maxCols + 0.35f)).coerceIn(
            with(density) { 54.dp.toPx() },
            with(density) { 84.dp.toPx() }
        )
        val iconSizeDp = with(density) { iconSizePx.toDp() }
        val cellSize = iconSizePx * 1.02f
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
        val overlayBlurActive = longPressedApp != null && blurEnabled && !suppressHeavyEffects

        Box(
            modifier = Modifier
                .fillMaxSize()
                .platformBlur(16f, overlayBlurActive)
                .pointerInput(apps, positions, scrollOffset.value) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startOffset ->
                            val startIndex = findNearestHoneycombIndex(
                                pointer = startOffset,
                                positions = positions,
                                screenCenterX = screenCenterX,
                                screenCenterY = screenCenterY + scrollOffset.value,
                                maxDistance = iconSizePx * 0.7f
                            )
                            if (startIndex != null) {
                                dragFromIndex = startIndex
                                dragCurrentIndex = startIndex
                                dragOffset = Offset.Zero
                                pressedAppKey = apps.getOrNull(startIndex)?.let { "${it.packageName}/${it.activityName}" }
                                vibrateHaptic(context)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val fromIndex = dragFromIndex ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            dragOffset += Offset(dragAmount.x, dragAmount.y)
                            val dragTarget = findNearestHoneycombIndex(
                                pointer = dragPointerCenter(
                                    index = fromIndex,
                                    positions = positions,
                                    screenCenterX = screenCenterX,
                                    screenCenterY = screenCenterY + scrollOffset.value,
                                    dragOffset = dragOffset
                                ),
                                positions = positions,
                                screenCenterX = screenCenterX,
                                screenCenterY = screenCenterY + scrollOffset.value,
                                maxDistance = cellSize * 0.95f
                            )
                            dragCurrentIndex = dragTarget ?: fromIndex
                        },
                        onDragEnd = {
                            val from = dragFromIndex
                            val to = dragCurrentIndex
                            if (from != null && to != null && from != to) {
                                onReorder(from, to)
                            }
                            dragFromIndex = null
                            dragCurrentIndex = null
                            dragOffset = Offset.Zero
                            pressedAppKey = null
                        },
                        onDragCancel = {
                            dragFromIndex = null
                            dragCurrentIndex = null
                            dragOffset = Offset.Zero
                            pressedAppKey = null
                        }
                    )
                }
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
                            val dampedDrag = if (overscroll != 0f) dragAmount.y * 0.28f else dragAmount.y
                            scope.launch { scrollOffset.snapTo(current + dampedDrag) }
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().y
                            val current = scrollOffset.value
                            if (current >= maxScroll - iconSizePx * 0.45f && velocity > 800f) {
                                onScrollToTop()
                                return@detectDragGestures
                            }
                            if (current < minScroll || current > maxScroll) {
                                scope.launch {
                                    scrollOffset.animateTo(
                                        current.coerceIn(minScroll, maxScroll),
                                        spring(dampingRatio = 0.64f, stiffness = 360f)
                                    )
                                }
                            } else {
                                scope.launch {
                                    scrollOffset.animateDecay(velocity, exponentialDecay()) {
                                        if (value < minScroll || value > maxScroll) {
                                            scope.launch {
                                                scrollOffset.animateTo(
                                                    value.coerceIn(minScroll, maxScroll),
                                                    spring(dampingRatio = 0.64f, stiffness = 360f)
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

                val topStrength = edgeStrength(posY, topFadePx)
                val bottomStrength = edgeStrength(screenHeightPx - posY, bottomFadePx)
                val topBlur = topStrength * topBlurRadiusDp
                val bottomBlur = bottomStrength * bottomBlurRadiusDp
                val itemBlur = maxOf(topBlur, bottomBlur)
                val appKey = "${app.packageName}/${app.activityName}"
                val isDragged = dragFromIndex == index
                val activePressKey = if (isDragged) appKey else pressedAppKey

                key(appKey) {
                    val targetNeighborScale = neighborPressOffset(
                        appKey = appKey,
                        pressedAppKey = activePressKey,
                        current = gridPos,
                        positions = positions,
                        apps = apps,
                        iconSizePx = iconSizePx,
                        cellSize = cellSize
                    )
                    val animatedNeighborScale by animateFloatAsState(
                        targetValue = targetNeighborScale,
                        animationSpec = tween(durationMillis = 180),
                        label = "neighbor_scale"
                    )
                    AppBubble(
                        icon = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && blurEnabled && effectiveEdgeBlur && itemBlur > 0.5f) {
                            app.cachedBlurredIcon
                        } else {
                            app.cachedIcon
                        },
                        size = iconSizeDp,
                        onClick = {
                            val sy = scrollOffset.value
                            val sx = screenCenterX + gridPos.x
                            val syPos = screenCenterY + gridPos.y + sy
                            onAppClick(app, Offset(sx / screenWidthPx, syPos / screenHeightPx))
                        },
                        onLongClick = {
                            if (dragFromIndex == null) {
                                onLongClick(app)
                                longPressedApp = app
                            }
                        },
                        forcePressed = isDragged,
                        onPressedChange = { pressed ->
                            if (!isDragged) {
                                pressedAppKey = if (pressed) appKey else pressedAppKey.takeUnless { it == appKey }
                            }
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                val sy = scrollOffset.value
                                val posX = screenCenterX + gridPos.x
                                val pY = screenCenterY + gridPos.y + sy
                                translationX = posX - iconSizePx / 2
                                translationY = pY - iconSizePx / 2
                                if (isDragged) {
                                    translationX += dragOffset.x
                                    translationY += dragOffset.y
                                }

                                val dx = posX - screenCenterX
                                val dy = pY - screenCenterY
                                val dist = sqrt(dx * dx + dy * dy)
                                val scale = fisheyeScale(dist, screenRadius * 1.65f, minScale = 0.58f)
                                val neighborScale = 1f - animatedNeighborScale
                                scaleX = scale * neighborScale
                                scaleY = scale * neighborScale
                                alpha = scale.coerceIn(0.24f, 1f)
                            }
                            .platformBlur(itemBlur, blurEnabled && effectiveEdgeBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
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
        AppShortcutOverlay(app = app, blurEnabled = blurEnabled, onDismiss = { longPressedApp = null })
    }
}

private fun edgeStrength(position: Float, leadingRange: Float): Float {
    if (leadingRange <= 0f) return 0f
    if (position <= 0f || position >= leadingRange) return 0f
    return (1f - (position / leadingRange)).coerceIn(0f, 1f)
}

private fun neighborPressOffset(
    appKey: String,
    pressedAppKey: String?,
    current: Offset,
    positions: List<Offset>,
    apps: List<AppInfo>,
    iconSizePx: Float,
    cellSize: Float
): Float {
    if (pressedAppKey == null) return 0f
    if (pressedAppKey == appKey) return 0f
    val pressedIndex = apps.indexOfFirst { "${it.packageName}/${it.activityName}" == pressedAppKey }
    val pressedPos = positions.getOrNull(pressedIndex) ?: return 0f
    val ddx = current.x - pressedPos.x
    val ddy = current.y - pressedPos.y
    val distance = sqrt(ddx * ddx + ddy * ddy)
    val range = cellSize * 1.75f
    return 0.08f * (1f - distance / range).coerceIn(0f, 1f)
}

private fun findNearestHoneycombIndex(
    pointer: Offset,
    positions: List<Offset>,
    screenCenterX: Float,
    screenCenterY: Float,
    maxDistance: Float
): Int? {
    var bestIndex: Int? = null
    var bestDistance = Float.MAX_VALUE
    positions.forEachIndexed { index, position ->
        val dx = pointer.x - (screenCenterX + position.x)
        val dy = pointer.y - (screenCenterY + position.y)
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < bestDistance && distance <= maxDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

private fun dragPointerCenter(
    index: Int,
    positions: List<Offset>,
    screenCenterX: Float,
    screenCenterY: Float,
    dragOffset: Offset
): Offset {
    val base = positions[index]
    return Offset(
        x = screenCenterX + base.x + dragOffset.x,
        y = screenCenterY + base.y + dragOffset.y
    )
}
