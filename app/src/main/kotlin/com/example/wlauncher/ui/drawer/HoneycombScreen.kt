package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHoneycombRows
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

private const val HONEYCOMB_PRESS_SCALE = 0.95f
private const val HONEYCOMB_DRAG_SCALE = 1.06f
private const val HONEYCOMB_SETTLE_PULSE_MS = 220L

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    narrowCols: Int = 4,
    iconScaleMultiplier: Float = 1f,
    fisheyeEnabled: Boolean = true,
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
    val viewConfiguration = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var pressedAppKey by remember { mutableStateOf<String?>(null) }
    var settlePulseKey by remember { mutableStateOf<String?>(null) }
    var dragPreview by remember { mutableStateOf(DrawerPreviewOrderState(apps.map { it.componentKey })) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var settleTargetOffset by remember { mutableStateOf<Offset?>(null) }
    var dragPointer by remember { mutableStateOf<Offset?>(null) }
    var autoScrollVelocity by remember { mutableFloatStateOf(0f) }

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

        val positions = remember(apps.size, narrowCols, cellSize) {
            generateHoneycombRows(apps.size, narrowCols, cellSize)
        }
        val minGridY = positions.minOfOrNull { it.y } ?: 0f
        val maxGridY = positions.maxOfOrNull { it.y } ?: 0f
        val maxScroll = -minGridY
        val minScroll = -maxGridY
        val clampedInitialScroll = initialScrollOffset.coerceIn(minScroll, maxScroll)
        val scrollOffset = remember { androidx.compose.animation.core.Animatable(clampedInitialScroll) }

        val appKeys = apps.map { it.componentKey }
        val autoScrollSpec = remember(screenHeightPx, iconSizePx) {
            DrawerAutoScrollSpec(
                viewportHeight = screenHeightPx,
                thresholdPx = iconSizePx * 1.15f,
                maxVelocityPxPerSecond = iconSizePx * 14f,
                accelerationPxPerSecond2 = iconSizePx * 46f,
                decelerationPxPerSecond2 = iconSizePx * 62f
            )
        }

        LaunchedEffect(appKeys) {
            dragPreview = if (dragPreview.isDragging) {
                dragPreview.copy(baseKeys = appKeys)
            } else {
                DrawerPreviewOrderState(appKeys)
            }
        }
        LaunchedEffect(minScroll, maxScroll) {
            scrollOffset.snapTo(scrollOffset.value.coerceIn(minScroll, maxScroll))
        }
        LaunchedEffect(scrollOffset) {
            snapshotFlow { scrollOffset.value }.collectLatest(onScrollOffsetChange)
        }

        fun updateDragTarget(pointerPosition: Offset) {
            val targetIndex = findNearestHoneycombIndex(
                pointer = pointerPosition,
                positions = positions,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY + scrollOffset.value,
                maxDistance = cellSize * 1.08f
            ) ?: return
            dragPreview = dragPreview.updateTarget(targetIndex)
        }

        fun beginDrag(index: Int, pointerPosition: Offset) {
            val app = apps.getOrNull(index) ?: return
            if (dragPreview.isDragging) return
            menuApp = null
            dragOffset = Offset.Zero
            settleTargetOffset = null
            dragPointer = pointerPosition
            pressedAppKey = app.componentKey
            dragPreview = dragPreview.beginDrag(app.componentKey, index)
            vibrateHaptic(context)
        }

        fun updateDrag(delta: Offset, pointerPosition: Offset) {
            if (!dragPreview.isDragging) return
            dragOffset += delta
            dragPointer = pointerPosition
            updateDragTarget(pointerPosition)
        }

        fun clearDragState() {
            val dragKey = dragPreview.draggingKey
            dragPreview = dragPreview.clearDrag()
            dragOffset = Offset.Zero
            settleTargetOffset = null
            dragPointer = null
            autoScrollVelocity = 0f
            pressedAppKey = null
            if (dragKey != null) {
                settlePulseKey = dragKey
                scope.launch {
                    delay(HONEYCOMB_SETTLE_PULSE_MS)
                    if (settlePulseKey == dragKey) {
                        settlePulseKey = null
                    }
                }
            }
        }

        fun finishDrag() {
            if (!dragPreview.isDragging) return
            val fromIndex = dragPreview.dragFromIndex
            val toIndex = dragPreview.dragTargetIndex
            val source = positions.getOrNull(fromIndex) ?: Offset.Zero
            val target = positions.getOrNull(toIndex) ?: source
            val targetOffset = target - source
            if ((targetOffset - dragOffset).getDistance() <= 1f) {
                if (fromIndex != toIndex) onReorder(fromIndex, toIndex)
                clearDragState()
            } else {
                settleTargetOffset = targetOffset
            }
        }

        val animatedDragOffset by animateOffsetAsState(
            targetValue = settleTargetOffset ?: dragOffset,
            animationSpec = if (settleTargetOffset != null) {
                spring(dampingRatio = 0.86f, stiffness = 520f)
            } else {
                snap()
            },
            finishedListener = { finished ->
                val target = settleTargetOffset ?: return@animateOffsetAsState
                if ((finished - target).getDistance() <= 1f) {
                    val fromIndex = dragPreview.dragFromIndex
                    val toIndex = dragPreview.dragTargetIndex
                    if (fromIndex != toIndex) onReorder(fromIndex, toIndex)
                    clearDragState()
                }
            },
            label = "honeycomb_drag_offset"
        )

        LaunchedEffect(dragPreview.isDragging, autoScrollSpec) {
            if (!dragPreview.isDragging) {
                autoScrollVelocity = 0f
                return@LaunchedEffect
            }
            var lastFrame = 0L
            while (isActive && dragPreview.isDragging) {
                withFrameNanos { frameTime ->
                    val deltaSeconds = if (lastFrame == 0L) 0f else (frameTime - lastFrame) / 1_000_000_000f
                    lastFrame = frameTime
                    val pointer = dragPointer
                    val targetVelocity = pointer?.let { targetAutoScrollVelocity(it.y, autoScrollSpec) } ?: 0f
                    autoScrollVelocity = stepAutoScrollVelocity(
                        current = autoScrollVelocity,
                        target = targetVelocity,
                        deltaSeconds = deltaSeconds,
                        spec = autoScrollSpec
                    )
                    if (deltaSeconds <= 0f || abs(autoScrollVelocity) < 1f) return@withFrameNanos
                    val delta = -autoScrollVelocity * deltaSeconds
                    if (delta == 0f) return@withFrameNanos
                    val next = (scrollOffset.value + delta).coerceIn(minScroll, maxScroll)
                    val applied = next - scrollOffset.value
                    if (abs(applied) < 0.01f) return@withFrameNanos
                    scope.launch { scrollOffset.snapTo(next) }
                    dragOffset += Offset(0f, applied)
                    dragPointer?.let(::updateDragTarget)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
                .onRotaryScrollEvent { event ->
                    scope.launch {
                        val next = (scrollOffset.value + event.verticalScrollPixels * 0.6f).coerceIn(minScroll, maxScroll)
                        scrollOffset.snapTo(next)
                    }
                    true
                }
                .pointerInput(appKeys, positions, minScroll, maxScroll, menuApp) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            if (dragPreview.isDragging || menuApp != null) return@detectDragGestures
                            scope.launch { scrollOffset.stop() }
                            velocityTracker.resetTracking()
                        },
                        onDrag = { change, dragAmount ->
                            if (dragPreview.isDragging || menuApp != null) return@detectDragGestures
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
                            scope.launch { scrollOffset.snapTo((current + dampedDrag).coerceIn(minScroll - iconSizePx * 0.45f, maxScroll + iconSizePx * 0.45f)) }
                        },
                        onDragEnd = {
                            if (dragPreview.isDragging || menuApp != null) return@detectDragGestures
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
                                        spring(dampingRatio = 0.68f, stiffness = 380f)
                                    )
                                }
                            } else {
                                scope.launch {
                                    scrollOffset.animateDecay(velocity, exponentialDecay()) {
                                        if (value < minScroll || value > maxScroll) {
                                            scope.launch {
                                                scrollOffset.animateTo(
                                                    value.coerceIn(minScroll, maxScroll),
                                                    spring(dampingRatio = 0.68f, stiffness = 380f)
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
            val previewSlotByKey = remember(dragPreview.settledKeys) {
                dragPreview.settledKeys.withIndex().associate { it.value to it.index }
            }
            val renderOrder = remember(apps, dragPreview.draggingKey) {
                apps.indices.sortedBy { if (apps[it].componentKey == dragPreview.draggingKey) 1 else 0 }
            }

            renderOrder.forEach { appIndex ->
                val app = apps[appIndex]
                val appKey = app.componentKey
                val isDragged = dragPreview.draggingKey == appKey
                val sourceIndex = if (isDragged) dragPreview.dragFromIndex else appIndex
                val slotIndex = previewSlotByKey[appKey] ?: appIndex
                val sourcePos = positions.getOrNull(sourceIndex) ?: Offset.Zero
                val slotPos = positions.getOrNull(slotIndex) ?: sourcePos
                val displayPos = if (isDragged) sourcePos else slotPos
                val posY = screenCenterY + displayPos.y + scrollOffset.value
                if (posY < -iconSizePx * 1.5f || posY > screenHeightPx + iconSizePx * 1.5f) return@forEach

                val pulseCenterKey = if (dragPreview.isDragging) null else (settlePulseKey ?: pressedAppKey)
                val pulseMotion = honeycombPulseMotion(
                    appKey = appKey,
                    centerKey = pulseCenterKey,
                    current = slotPos,
                    positions = positions,
                    previewKeys = dragPreview.settledKeys,
                    iconSizePx = iconSizePx,
                    cellSize = cellSize
                )
                val pressed = isDragged || pressedAppKey == appKey || menuApp?.componentKey == appKey

                key(appKey) {
                    AppBubble(
                        icon = app.cachedIcon,
                        size = iconSizeDp,
                        pressed = pressed,
                        scaleTargetWhenPressed = if (isDragged) HONEYCOMB_DRAG_SCALE else HONEYCOMB_PRESS_SCALE,
                        modifier = Modifier
                            .pointerInput(appKey, dragPreview.draggingKey, menuApp, scrollOffset.value) {
                                awaitPointerEventScope {
                                    runDrawerLongPressSequence(
                                        touchSlop = touchSlop,
                                        onShowMenu = {
                                            if (!dragPreview.isDragging) {
                                                menuApp = app
                                                pressedAppKey = appKey
                                                onLongClick(app)
                                            }
                                        },
                                        onMenuToDrag = { pointerPosition ->
                                            menuApp = null
                                            beginDrag(appIndex, pointerPosition)
                                        },
                                        onBeginDrag = { pointerPosition ->
                                            beginDrag(appIndex, pointerPosition)
                                        },
                                        onDragDelta = { delta, pointerPosition ->
                                            updateDrag(delta, pointerPosition)
                                        },
                                        onFinishDrag = { finishDrag() },
                                        onTap = {
                                            val currentScroll = scrollOffset.value
                                            val currentPos = positions.getOrNull(slotIndex) ?: Offset.Zero
                                            val x = screenCenterX + currentPos.x
                                            val y = screenCenterY + currentPos.y + currentScroll
                                            onAppClick(app, Offset(x / screenWidthPx, y / screenHeightPx))
                                        },
                                        onPressStateChange = { active ->
                                            if (!dragPreview.isDragging && menuApp == null) {
                                                pressedAppKey = if (active) appKey else pressedAppKey.takeUnless { it == appKey }
                                            }
                                        }
                                    )
                                }
                            }
                            .graphicsLayer {
                                translationX = screenCenterX + displayPos.x - iconSizePx / 2f
                                translationY = screenCenterY + displayPos.y + scrollOffset.value - iconSizePx / 2f
                                if (isDragged) {
                                    translationX += animatedDragOffset.x
                                    translationY += animatedDragOffset.y
                                    shadowElevation = 20.dp.toPx()
                                } else {
                                    translationX += pulseMotion.x
                                    translationY += pulseMotion.y
                                    shadowElevation = 0f
                                }

                                val dx = (screenCenterX + slotPos.x) - screenCenterX
                                val dy = (screenCenterY + slotPos.y + scrollOffset.value) - screenCenterY
                                val distance = sqrt(dx * dx + dy * dy)
                                val baseScale = if (fisheyeEnabled) {
                                    fisheyeScale(distance, screenRadius * 1.6f, minScale = 0.58f)
                                } else {
                                    1f
                                }
                                scaleX = baseScale * pulseMotion.scale
                                scaleY = baseScale * pulseMotion.scale
                                alpha = baseScale.coerceIn(0.26f, 1f)
                            }
                    )
                }
            }
        }

        DrawerTopBlurMask(
            height = topFadeRangeDp.dp,
            blurRadiusDp = blurRadiusDp,
            enabled = edgeBlurEnabled && !suppressHeavyEffects,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        DrawerBottomBlurMask(
            height = bottomFadeRangeDp.dp,
            blurRadiusDp = blurRadiusDp,
            enabled = edgeBlurEnabled && !suppressHeavyEffects,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    menuApp?.let { app ->
        AppShortcutOverlay(
            app = app,
            blurEnabled = menuBlurEnabled,
            blurRadiusDp = blurRadiusDp,
            onDismiss = {
                if (menuApp?.componentKey == app.componentKey) {
                    menuApp = null
                    pressedAppKey = null
                }
            }
        )
    }
}

private data class HoneycombPulseMotion(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f
)

private fun honeycombPulseMotion(
    appKey: String,
    centerKey: String?,
    current: Offset,
    positions: List<Offset>,
    previewKeys: List<String>,
    iconSizePx: Float,
    cellSize: Float
): HoneycombPulseMotion {
    if (centerKey == null || centerKey == appKey) return HoneycombPulseMotion()
    val centerIndex = previewKeys.indexOf(centerKey)
    val centerPos = positions.getOrNull(centerIndex) ?: return HoneycombPulseMotion()
    val dx = centerPos.x - current.x
    val dy = centerPos.y - current.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance <= 0.001f) return HoneycombPulseMotion()
    val progress = (1f - distance / (cellSize * 1.75f)).coerceIn(0f, 1f)
    if (progress <= 0f) return HoneycombPulseMotion()
    val sink = iconSizePx * 0.06f * progress
    return HoneycombPulseMotion(
        x = dx / distance * iconSizePx * 0.08f * progress,
        y = dy / distance * iconSizePx * 0.08f * progress + sink,
        scale = 1f - 0.035f * progress
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
