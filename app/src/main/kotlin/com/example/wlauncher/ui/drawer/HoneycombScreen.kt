package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val HONEYCOMB_PRESS_DELAY_MS = 220
private const val HONEYCOMB_PRESS_DURATION_MS = 240

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    narrowCols: Int = 4,
    iconScaleMultiplier: Float = 1f,
    topFadeRangeDp: Int = 56,
    bottomFadeRangeDp: Int = 56,
    blurRadiusDp: Int = 4,
    initialScrollOffset: Float = 0f,
    onScrollOffsetChange: (Float) -> Unit = {},
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    menuBlurEnabled: Boolean = true,
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
        ) * iconScaleMultiplier.coerceIn(0.8f, 1.35f)
        val iconSizeDp = with(density) { iconSizePx.toDp() }
        val cellSize = iconSizePx * 1.02f
        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }

        val minGridY = positions.minOfOrNull { it.y } ?: 0f
        val maxGridY = positions.maxOfOrNull { it.y } ?: 0f
        val maxScroll = -minGridY
        val minScroll = -maxGridY
        val scrollOffset = remember { Animatable(initialScrollOffset.coerceIn(minScroll, maxScroll)) }
        val scope = rememberCoroutineScope()

        fun clearDrag() {
            dragFromIndex = null
            dragCurrentIndex = null
            dragOffset = Offset.Zero
            pressedAppKey = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                                pressedAppKey = apps.getOrNull(startIndex)?.componentKey
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
                            clearDrag()
                        },
                        onDragCancel = { clearDrag() }
                    )
                }
                .pointerInput(apps, positions, minScroll, maxScroll) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            if (dragFromIndex != null) return@detectDragGestures
                            scope.launch { scrollOffset.stop() }
                            velocityTracker.resetTracking()
                        },
                        onDrag = { change, dragAmount ->
                            if (dragFromIndex != null) return@detectDragGestures
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
                            if (dragFromIndex != null) return@detectDragGestures
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
            onScrollOffsetChange(currentScroll)

            apps.forEachIndexed { index, app ->
                if (index >= positions.size) return@forEachIndexed
                val gridPos = positions[index]
                val posX = screenCenterX + gridPos.x
                val posY = screenCenterY + gridPos.y + currentScroll
                val appKey = app.componentKey
                val isDragged = dragFromIndex == index
                val motion = neighborPressMotion(
                    appKey = appKey,
                    pressedAppKey = if (isDragged) appKey else pressedAppKey,
                    current = gridPos,
                    positions = positions,
                    apps = apps,
                    iconSizePx = iconSizePx,
                    cellSize = cellSize
                )

                key(appKey) {
                    val animatedNeighborScale by animateFloatAsState(
                        targetValue = 1f - motion.scaleReduction,
                        animationSpec = tween(durationMillis = 260, delayMillis = if (motion.scaleReduction > 0f) 180 else 0),
                        label = "neighbor_scale"
                    )
                    val animatedNeighborShiftX by animateFloatAsState(
                        targetValue = motion.shiftX,
                        animationSpec = tween(durationMillis = 280, delayMillis = if (motion.shiftX != 0f) 180 else 0),
                        label = "neighbor_shift_x"
                    )
                    val animatedNeighborShiftY by animateFloatAsState(
                        targetValue = motion.shiftY,
                        animationSpec = tween(durationMillis = 280, delayMillis = if (motion.shiftY != 0f) 180 else 0),
                        label = "neighbor_shift_y"
                    )

                    AppBubble(
                        icon = app.cachedIcon,
                        size = iconSizeDp,
                        onClick = {
                            onAppClick(app, Offset(posX / screenWidthPx, posY / screenHeightPx))
                        },
                        onLongClick = {
                            if (dragFromIndex == null) {
                                pressedAppKey = appKey
                                onLongClick(app)
                                longPressedApp = app
                            }
                        },
                        forcePressed = isDragged,
                        forceScaleTarget = 1.06f,
                        pressScaleTarget = 0.88f,
                        pressAnimationDelayMillis = if (isDragged) 0 else HONEYCOMB_PRESS_DELAY_MS,
                        pressAnimationDurationMillis = HONEYCOMB_PRESS_DURATION_MS,
                        onPressedChange = { pressed ->
                            if (!isDragged) {
                                pressedAppKey = if (pressed) appKey else pressedAppKey.takeUnless { it == appKey }
                            }
                        },
                        modifier = Modifier.graphicsLayer {
                            translationX = posX - iconSizePx / 2f
                            translationY = posY - iconSizePx / 2f
                            if (isDragged) {
                                translationX += dragOffset.x
                                translationY += dragOffset.y
                                shadowElevation = 18.dp.toPx()
                            } else {
                                translationX += animatedNeighborShiftX
                                translationY += animatedNeighborShiftY
                                shadowElevation = 0f
                            }
                            val dx = posX - screenCenterX
                            val dy = posY - screenCenterY
                            val dist = sqrt(dx * dx + dy * dy)
                            val scale = fisheyeScale(dist, screenRadius * 1.65f, minScale = 0.58f)
                            scaleX = scale * animatedNeighborScale
                            scaleY = scale * animatedNeighborScale
                            alpha = scale.coerceIn(0.24f, 1f)
                        }
                    )
                }
            }
        }

        if (topFadeRangeDp > 0) {
            DrawerTopBlurMask(
                height = topFadeRangeDp.dp,
                blurRadiusDp = blurRadiusDp,
                enabled = edgeBlurEnabled && !suppressHeavyEffects,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        if (bottomFadeRangeDp > 0) {
            DrawerBottomBlurMask(
                height = bottomFadeRangeDp.dp,
                blurRadiusDp = blurRadiusDp,
                enabled = edgeBlurEnabled && !suppressHeavyEffects,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    longPressedApp?.let { app ->
        AppShortcutOverlay(
            app = app,
            blurEnabled = menuBlurEnabled,
            blurRadiusDp = blurRadiusDp,
            onDismiss = { longPressedApp = null }
        )
    }
}

private fun neighborPressMotion(
    appKey: String,
    pressedAppKey: String?,
    current: Offset,
    positions: List<Offset>,
    apps: List<AppInfo>,
    iconSizePx: Float,
    cellSize: Float
): HoneycombNeighborMotion {
    if (pressedAppKey == null || pressedAppKey == appKey) return HoneycombNeighborMotion()
    val pressedIndex = apps.indexOfFirst { it.componentKey == pressedAppKey }
    val pressedPos = positions.getOrNull(pressedIndex) ?: return HoneycombNeighborMotion()
    val dx = pressedPos.x - current.x
    val dy = pressedPos.y - current.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance <= 0.001f) return HoneycombNeighborMotion()
    val range = cellSize * 1.9f
    val progress = (1f - distance / range).coerceIn(0f, 1f)
    if (progress <= 0f) return HoneycombNeighborMotion()

    val pullDistance = iconSizePx * 0.18f * progress
    val sinkDistance = iconSizePx * 0.11f * progress
    return HoneycombNeighborMotion(
        scaleReduction = 0.08f * progress,
        shiftX = dx / distance * pullDistance,
        shiftY = dy / distance * pullDistance + sinkDistance
    )
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

private data class HoneycombNeighborMotion(
    val scaleReduction: Float = 0f,
    val shiftX: Float = 0f,
    val shiftY: Float = 0f
)
