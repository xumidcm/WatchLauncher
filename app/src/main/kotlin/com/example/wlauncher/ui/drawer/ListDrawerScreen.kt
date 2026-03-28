package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.data.model.AppInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val LIST_PRESS_SCALE = 0.97f
private const val LIST_DRAG_SCALE = 1.04f
private const val LIST_SETTLE_PULSE_MS = 220L

@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    iconSize: Dp = 48.dp,
    iconScaleMultiplier: Float = 1f,
    menuBlurEnabled: Boolean = true,
    blurRadiusDp: Int = 4,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemOffset: Int = 0,
    onScrollPositionChange: (Int, Int) -> Unit = { _, _ -> },
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemOffset
    )
    val density = LocalDensity.current
    val context = LocalContext.current
    val viewConfiguration = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var pressedAppKey by remember { mutableStateOf<String?>(null) }
    var settlePulseKey by remember { mutableStateOf<String?>(null) }
    var dragPreview by remember { mutableStateOf(DrawerPreviewOrderState(apps.map { it.componentKey })) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var settleTargetOffsetY by remember { mutableStateOf<Float?>(null) }
    var dragPointer by remember { mutableStateOf<Offset?>(null) }
    var autoScrollVelocity by remember { mutableFloatStateOf(0f) }

    val appKeys = apps.map { it.componentKey }
    LaunchedEffect(appKeys) {
        dragPreview = if (dragPreview.isDragging) {
            dragPreview.copy(baseKeys = appKeys)
        } else {
            DrawerPreviewOrderState(appKeys)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) -> onScrollPositionChange(index, offset) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(
                    remember(listState, dragPreview.isDragging, menuApp) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (source != NestedScrollSource.Drag || dragPreview.isDragging || menuApp != null) return Offset.Zero
                                return consumeListOverscroll(available.y, listState, overscroll = null, scope = scope)
                            }

                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source != NestedScrollSource.Drag || dragPreview.isDragging || menuApp != null) return Offset.Zero
                                return consumeListOverscroll(available.y, listState, overscroll = null, scope = scope)
                            }

                            override suspend fun onPreFling(available: Velocity): Velocity {
                                if (dragPreview.isDragging || menuApp != null) return Velocity.Zero
                                val atTop = !listState.canScrollBackward
                                if (atTop && available.y > 1200f) {
                                    onScrollToTop()
                                    return available
                                }
                                return Velocity.Zero
                            }
                        }
                    }
                )
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent { event ->
                    scope.launch { listState.scrollBy(-event.verticalScrollPixels) }
                    true
                }
        ) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenCenterY = screenHeightPx / 2f
            val scaledIconSize = iconSize * iconScaleMultiplier.coerceIn(0.8f, 1.35f)
            val estimatedItemHeight = scaledIconSize.coerceAtLeast(48.dp) + 20.dp
            val topPadding = 24.dp
            val bottomPadding = 56.dp
            val touchSlop = viewConfiguration.touchSlop
            val itemCenters = remember { mutableMapOf<String, Float>() }
            val itemHeights = remember { mutableMapOf<String, Float>() }
            val autoScrollSpec = remember(screenHeightPx, scaledIconSize) {
                DrawerAutoScrollSpec(
                    viewportHeight = screenHeightPx,
                    thresholdPx = with(density) { 72.dp.toPx() },
                    maxVelocityPxPerSecond = with(density) { 640.dp.toPx() },
                    accelerationPxPerSecond2 = with(density) { 1800.dp.toPx() },
                    decelerationPxPerSecond2 = with(density) { 2600.dp.toPx() }
                )
            }

            fun beginDrag(index: Int, pointerPosition: Offset) {
                val app = apps.getOrNull(index) ?: return
                if (dragPreview.isDragging) return
                menuApp = null
                dragOffsetY = 0f
                settleTargetOffsetY = null
                dragPointer = pointerPosition
                pressedAppKey = app.componentKey
                dragPreview = dragPreview.beginDrag(app.componentKey, index)
                vibrateHaptic(context)
            }

            fun updateDragTarget(pointerPosition: Offset) {
                val targetIndex = findNearestListIndex(
                    pointerY = pointerPosition.y,
                    itemCenters = apps.mapIndexedNotNull { index, app ->
                        itemCenters[app.componentKey]?.let { index to it }
                    }.toMap(),
                    maxDistance = Float.MAX_VALUE
                ) ?: return
                dragPreview = dragPreview.updateTarget(targetIndex)
            }

            fun updateDrag(delta: Offset, pointerPosition: Offset) {
                if (!dragPreview.isDragging) return
                dragOffsetY += delta.y
                dragPointer = pointerPosition
                updateDragTarget(pointerPosition)
            }

            fun clearDragState() {
                val dragKey = dragPreview.draggingKey
                dragPreview = dragPreview.clearDrag()
                dragOffsetY = 0f
                settleTargetOffsetY = null
                dragPointer = null
                autoScrollVelocity = 0f
                pressedAppKey = null
                if (dragKey != null) {
                    settlePulseKey = dragKey
                    scope.launch {
                        kotlinx.coroutines.delay(LIST_SETTLE_PULSE_MS)
                        if (settlePulseKey == dragKey) {
                            settlePulseKey = null
                        }
                    }
                }
            }

            fun finishDrag() {
                if (!dragPreview.isDragging) return
                val draggingKey = dragPreview.draggingKey ?: return
                val sourceCenter = itemCenters[draggingKey] ?: dragPointer?.y ?: 0f
                val targetKey = dragPreview.settledKeys.getOrNull(dragPreview.dragTargetIndex)
                val targetCenter = targetKey?.let { itemCenters[it] } ?: sourceCenter
                val targetOffset = targetCenter - sourceCenter
                if (abs(targetOffset - dragOffsetY) <= 1f) {
                    if (dragPreview.dragFromIndex != dragPreview.dragTargetIndex) {
                        onReorder(dragPreview.dragFromIndex, dragPreview.dragTargetIndex)
                    }
                    clearDragState()
                } else {
                    settleTargetOffsetY = targetOffset
                }
            }

            val animatedDragOffsetY by animateFloatAsState(
                targetValue = settleTargetOffsetY ?: dragOffsetY,
                animationSpec = if (settleTargetOffsetY != null) {
                    spring(dampingRatio = 0.86f, stiffness = 520f)
                } else {
                    snap()
                },
                finishedListener = { finished ->
                    val target = settleTargetOffsetY ?: return@animateFloatAsState
                    if (abs(finished - target) <= 1f) {
                        if (dragPreview.dragFromIndex != dragPreview.dragTargetIndex) {
                            onReorder(dragPreview.dragFromIndex, dragPreview.dragTargetIndex)
                        }
                        clearDragState()
                    }
                },
                label = "list_drag_offset"
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

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
                        val requested = autoScrollVelocity * deltaSeconds
                        if (requested == 0f) return@withFrameNanos
                        scope.launch {
                            val applied = listState.scrollBy(requested)
                            if (abs(applied) > 0.01f) {
                                dragOffsetY += applied
                                dragPointer?.let(::updateDragTarget)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = bottomPadding,
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(apps, key = { _, app -> app.componentKey }) { index, app ->
                    val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
                    val isPressedByClick by interactionSource.collectIsPressedAsState()
                    val isDragged = dragPreview.draggingKey == app.componentKey
                    val rowHeight = itemHeights[app.componentKey] ?: with(density) { estimatedItemHeight.toPx() }
                    val displacedTarget = listDisplacementForIndex(
                        index = index,
                        dragFromIndex = dragPreview.takeIf { it.isDragging }?.dragFromIndex,
                        dragCurrentIndex = dragPreview.takeIf { it.isDragging }?.dragTargetIndex,
                        dragRowShift = rowHeight + with(density) { 4.dp.toPx() }
                    )
                    val pulseMotion = listPulseMotion(
                        appKey = app.componentKey,
                        centerKey = if (dragPreview.isDragging) null else (settlePulseKey ?: pressedAppKey),
                        itemCenters = itemCenters,
                        itemHeights = itemHeights
                    )
                    val translationY by animateFloatAsState(
                        targetValue = if (isDragged) animatedDragOffsetY else displacedTarget + pulseMotion.shiftY,
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 460f),
                        label = "list_row_translation"
                    )
                    val pressedScale by animateFloatAsState(
                        targetValue = when {
                            isDragged -> LIST_DRAG_SCALE
                            pressedAppKey == app.componentKey || menuApp?.componentKey == app.componentKey || isPressedByClick -> LIST_PRESS_SCALE
                            else -> pulseMotion.scale
                        },
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 460f),
                        label = "list_row_scale"
                    )
                    val overlayAlpha by animateFloatAsState(
                        targetValue = if (isDragged || pressedAppKey == app.componentKey || isPressedByClick) 0.10f else 0f,
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
                        label = "list_row_overlay"
                    )

                    key(app.componentKey) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    val posY = coords.positionInRoot().y
                                    itemCenters[app.componentKey] = posY + coords.size.height / 2f
                                    itemHeights[app.componentKey] = coords.size.height.toFloat()
                                }
                                .pointerInput(app.componentKey, dragPreview.draggingKey, menuApp) {
                                    awaitPointerEventScope {
                                        runDrawerLongPressSequence(
                                            touchSlop = touchSlop,
                                            onShowMenu = {
                                                if (!dragPreview.isDragging) {
                                                    menuApp = app
                                                    pressedAppKey = app.componentKey
                                                    onLongClick(app)
                                                }
                                            },
                                            onMenuToDrag = { pointerPosition ->
                                                menuApp = null
                                                beginDrag(index, pointerPosition)
                                            },
                                            onBeginDrag = { pointerPosition ->
                                                beginDrag(index, pointerPosition)
                                            },
                                            onDragDelta = { delta, pointerPosition ->
                                                updateDrag(delta, pointerPosition)
                                            },
                                            onFinishDrag = { finishDrag() },
                                            onTap = {
                                                val centerY = itemCenters[app.componentKey] ?: screenCenterY
                                                onAppClick(app, Offset(0.15f, centerY / screenHeightPx))
                                            },
                                            onPressStateChange = { active ->
                                                if (!dragPreview.isDragging && menuApp == null) {
                                                    pressedAppKey = if (active) app.componentKey else pressedAppKey.takeUnless { it == app.componentKey }
                                                }
                                            }
                                        )
                                    }
                                }
                                .graphicsLayer {
                                    translationY = translationY
                                    scaleX = pressedScale
                                    scaleY = pressedScale
                                    alpha = if (isDragged) 0.98f else 1f
                                    shadowElevation = if (isDragged) 18.dp.toPx() else 0f
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = overlayAlpha))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = app.cachedIcon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(scaledIconSize)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = app.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        DrawerTopBlurMask(
            height = 56.dp,
            blurRadiusDp = blurRadiusDp,
            enabled = edgeBlurEnabled && !suppressHeavyEffects,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        DrawerBottomBlurMask(
            height = 60.dp,
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

private data class ListPulseMotion(
    val shiftY: Float = 0f,
    val scale: Float = 1f
)

private fun listPulseMotion(
    appKey: String,
    centerKey: String?,
    itemCenters: Map<String, Float>,
    itemHeights: Map<String, Float>
): ListPulseMotion {
    if (centerKey == null || centerKey == appKey) return ListPulseMotion()
    val currentCenter = itemCenters[appKey] ?: return ListPulseMotion()
    val centerY = itemCenters[centerKey] ?: return ListPulseMotion()
    val rowHeight = itemHeights[appKey] ?: return ListPulseMotion()
    val distance = abs(currentCenter - centerY)
    val progress = (1f - distance / (rowHeight * 1.8f)).coerceIn(0f, 1f)
    if (progress <= 0f) return ListPulseMotion()
    val direction = if (currentCenter > centerY) -1f else 1f
    return ListPulseMotion(
        shiftY = direction * rowHeight * 0.08f * progress,
        scale = 1f - 0.03f * progress
    )
}

private fun listDisplacementForIndex(
    index: Int,
    dragFromIndex: Int?,
    dragCurrentIndex: Int?,
    dragRowShift: Float
): Float {
    if (dragFromIndex == null || dragCurrentIndex == null || dragFromIndex == dragCurrentIndex) return 0f
    return when {
        dragCurrentIndex > dragFromIndex && index in (dragFromIndex + 1)..dragCurrentIndex -> -dragRowShift
        dragCurrentIndex < dragFromIndex && index in dragCurrentIndex until dragFromIndex -> dragRowShift
        else -> 0f
    }
}

private fun findNearestListIndex(
    pointerY: Float,
    itemCenters: Map<Int, Float>,
    maxDistance: Float
): Int? {
    var bestIndex: Int? = null
    var bestDistance = Float.MAX_VALUE
    itemCenters.forEach { (index, centerY) ->
        val distance = abs(centerY - pointerY)
        if (distance < bestDistance && distance <= maxDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

private fun consumeListOverscroll(
    availableY: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    overscroll: Animatable<Float, AnimationVector1D>?,
    scope: kotlinx.coroutines.CoroutineScope
): Offset {
    if (overscroll == null) return Offset.Zero
    val atTop = !listState.canScrollBackward
    val atBottom = !listState.canScrollForward
    val current = overscroll.value
    val next = when {
        availableY > 0f && atTop -> (current + availableY * 0.35f).coerceAtMost(180f)
        availableY < 0f && atBottom -> (current + availableY * 0.35f).coerceAtLeast(-180f)
        current > 0f && availableY < 0f -> (current + availableY).coerceAtLeast(0f)
        current < 0f && availableY > 0f -> (current + availableY).coerceAtMost(0f)
        else -> current
    }
    if (next == current) return Offset.Zero
    scope.launch { overscroll.snapTo(next) }
    return Offset(0f, availableY)
}
