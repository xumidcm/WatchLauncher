package com.example.wlauncher.ui.drawer

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.anim.platformBlur
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val HONEYCOMB_PRESS_DURATION_MS = 240

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    narrowCols: Int = 4,
    iconScaleMultiplier: Float = 1f,
    fisheyeEnabled: Boolean = true,
    topBlurRadiusDp: Int = 12,
    bottomBlurRadiusDp: Int = 12,
    topFadeRangeDp: Int = 56,
    bottomFadeRangeDp: Int = 56,
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    menuBlurEnabled: Boolean = true,
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewConfiguration = LocalViewConfiguration.current
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
        val touchSlop = viewConfiguration.touchSlop

        val maxCols = narrowCols + 1
        val availableWidth = screenWidthPx - with(density) { 20.dp.toPx() }
        val iconSizePx = (availableWidth / (maxCols + 0.35f)).coerceIn(
            with(density) { 54.dp.toPx() },
            with(density) { 84.dp.toPx() }
        ) * iconScaleMultiplier.coerceIn(0.8f, 1.35f)
        val iconSizeDp = with(density) { iconSizePx.toDp() }
        val cellSize = iconSizePx * 1.02f
        val topFadePx = with(density) { topFadeRangeDp.dp.toPx() }
        val bottomFadePx = with(density) { bottomFadeRangeDp.dp.toPx() }

        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }

        val minGridY = positions.minOfOrNull { it.y } ?: 0f
        val maxGridY = positions.maxOfOrNull { it.y } ?: 0f
        val maxScroll = -minGridY
        val minScroll = -maxGridY

        val scrollOffset = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val overlayBlurActive = longPressedApp != null && menuBlurEnabled && !suppressHeavyEffects

        fun beginDrag(index: Int) {
            if (dragFromIndex != null) return
            dragFromIndex = index
            dragCurrentIndex = index
            dragOffset = Offset.Zero
            pressedAppKey = apps.getOrNull(index)?.componentKey
            vibrateHaptic(context)
        }

        fun updateDrag(index: Int, delta: Offset, pointer: Offset) {
            dragOffset += delta
            val autoScroll = edgeAutoScrollDelta(
                pointerY = pointer.y,
                viewportHeight = screenHeightPx,
                threshold = iconSizePx * 1.18f,
                maxStep = iconSizePx * 0.22f
            )
            if (autoScroll != 0f) {
                scope.launch {
                    scrollOffset.snapTo((scrollOffset.value + autoScroll).coerceIn(minScroll, maxScroll))
                }
                dragOffset += Offset(0f, autoScroll)
            }
            val dragTarget = findNearestHoneycombIndex(
                pointer = pointer,
                positions = positions,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY + scrollOffset.value,
                maxDistance = cellSize * 0.95f
            )
            dragCurrentIndex = dragTarget ?: index
        }

        fun finishDrag() {
            val from = dragFromIndex
            val to = dragCurrentIndex
            if (from != null && to != null && from != to) {
                onReorder(from, to)
            }
            dragFromIndex = null
            dragCurrentIndex = null
            dragOffset = Offset.Zero
            pressedAppKey = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .platformBlur(16f, overlayBlurActive)
                .pointerInput(apps, positions, minScroll, maxScroll) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            if (dragFromIndex == null) {
                                scope.launch { scrollOffset.stop() }
                                velocityTracker.resetTracking()
                            }
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
            val visibleTop = -iconSizePx * 1.5f
            val visibleBottom = screenHeightPx + iconSizePx * 1.5f
            val autoScrollSpec = remember(screenHeightPx, iconSizePx) {
                DrawerEdgeAutoScrollSpec(
                    viewportHeight = screenHeightPx,
                    threshold = iconSizePx * 1.18f,
                    maxStep = iconSizePx * 0.22f
                )
            }

            val renderOrder = remember(apps, dragFromIndex) {
                apps.indices.sortedBy { if (it == dragFromIndex) 1 else 0 }
            }

            renderOrder.forEach { index ->
                if (index >= positions.size) return@forEach
                val app = apps[index]
                val gridPos = positions[index]
                val posY = screenCenterY + gridPos.y + currentScroll
                if (posY < visibleTop || posY > visibleBottom) return@forEach

                val topStrength = edgeStrength(posY, topFadePx)
                val bottomStrength = edgeStrength(screenHeightPx - posY, bottomFadePx)
                val topBlur = topStrength * topBlurRadiusDp
                val bottomBlur = bottomStrength * bottomBlurRadiusDp
                val itemBlur = maxOf(topBlur, bottomBlur)
                val appKey = app.componentKey
                val isDragged = dragFromIndex == index
                val activePressKey = if (isDragged) appKey else pressedAppKey
                val motion = neighborPressMotion(
                    appKey = appKey,
                    pressedAppKey = activePressKey,
                    current = gridPos,
                    positions = positions,
                    apps = apps,
                    iconSizePx = iconSizePx,
                    cellSize = cellSize
                )

                key(appKey) {
                    val animatedNeighborScale by animateFloatAsState(
                        targetValue = 1f - motion.scaleReduction,
                        animationSpec = tween(
                            durationMillis = 260,
                            delayMillis = if (motion.scaleReduction > 0f) 180 else 0
                        ),
                        label = "neighbor_scale"
                    )
                    val animatedNeighborShiftX by animateFloatAsState(
                        targetValue = motion.shiftX,
                        animationSpec = tween(
                            durationMillis = 280,
                            delayMillis = if (motion.shiftX != 0f) 180 else 0
                        ),
                        label = "neighbor_shift_x"
                    )
                    val animatedNeighborShiftY by animateFloatAsState(
                        targetValue = motion.shiftY,
                        animationSpec = tween(
                            durationMillis = 280,
                            delayMillis = if (motion.shiftY != 0f) 180 else 0
                        ),
                        label = "neighbor_shift_y"
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
                        onLongClick = null,
                        forcePressed = isDragged,
                        forceScaleTarget = 1.06f,
                        pressScaleTarget = 0.88f,
                        pressAnimationDelayMillis = 0,
                        pressAnimationDurationMillis = HONEYCOMB_PRESS_DURATION_MS,
                        onPressedChange = { pressed ->
                            if (!isDragged) {
                                pressedAppKey = if (pressed) appKey else pressedAppKey.takeUnless { it == appKey }
                            }
                        },
                        modifier = Modifier
                            .pointerInput(appKey, dragFromIndex, longPressedApp, scrollOffset.value) {
                                awaitPointerEventScope {
                                    runDrawerLongPressSequence(
                                        touchSlop = touchSlop,
                                        onShowMenu = {
                                            if (dragFromIndex == null) {
                                                longPressedApp = app
                                                onLongClick(app)
                                            }
                                        },
                                        onMenuToDrag = {
                                            longPressedApp = null
                                            beginDrag(index)
                                        },
                                        onBeginDrag = { beginDrag(index) },
                                        onDragDelta = { delta, pointerPosition ->
                                            dragOffset += delta
                                            val autoScroll = edgeAutoScrollDelta(pointerPosition.y, autoScrollSpec)
                                            if (autoScroll != 0f) {
                                                scope.launch {
                                                    scrollOffset.snapTo((scrollOffset.value + autoScroll).coerceIn(minScroll, maxScroll))
                                                }
                                                dragOffset += Offset(0f, autoScroll)
                                            }
                                            val updatedPointer = dragPointerCenter(
                                                index = index,
                                                positions = positions,
                                                screenCenterX = screenCenterX,
                                                screenCenterY = screenCenterY + scrollOffset.value,
                                                dragOffset = dragOffset
                                            )
                                            val dragTarget = findNearestHoneycombIndex(
                                                pointer = updatedPointer,
                                                positions = positions,
                                                screenCenterX = screenCenterX,
                                                screenCenterY = screenCenterY + scrollOffset.value,
                                                maxDistance = cellSize * 0.95f
                                            )
                                            dragCurrentIndex = dragTarget ?: index
                                        },
                                        onFinishDrag = { finishDrag() }
                                    )
                                }
                            }
                            .graphicsLayer {
                                val sy = scrollOffset.value
                                val posX = screenCenterX + gridPos.x
                                val pY = screenCenterY + gridPos.y + sy
                                translationX = posX - iconSizePx / 2f
                                translationY = pY - iconSizePx / 2f
                                if (isDragged) {
                                    translationX += dragOffset.x
                                    translationY += dragOffset.y
                                } else {
                                    translationX += animatedNeighborShiftX
                                    translationY += animatedNeighborShiftY
                                }

                                val dx = posX - screenCenterX
                                val dy = pY - screenCenterY
                                val dist = sqrt(dx * dx + dy * dy)
                                val scale = if (fisheyeEnabled) {
                                    fisheyeScale(dist, screenRadius * 1.72f, maxScale = 1.1f, minScale = 0.52f)
                                } else {
                                    1f
                                }
                                scaleX = scale * animatedNeighborScale
                                scaleY = scale * animatedNeighborScale
                                shadowElevation = if (isDragged) 18.dp.toPx() else 0f
                                alpha = scale.coerceIn(0.24f, 1f)
                            }
                            .platformBlur(
                                itemBlur,
                                blurEnabled && effectiveEdgeBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            )
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
        AppShortcutOverlay(app = app, blurEnabled = menuBlurEnabled, onDismiss = { longPressedApp = null })
    }
}

private fun edgeStrength(position: Float, leadingRange: Float): Float {
    if (leadingRange <= 0f) return 0f
    if (position <= 0f || position >= leadingRange) return 0f
    return (1f - (position / leadingRange)).coerceIn(0f, 1f)
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
    if (pressedAppKey == null || pressedAppKey == appKey) {
        return HoneycombNeighborMotion()
    }
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
