package com.example.wlauncher.ui.drawer

import android.os.Build
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.wlauncher.ui.anim.platformBlur
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    iconSize: Dp = 48.dp,
    iconScaleMultiplier: Float = 1f,
    menuBlurEnabled: Boolean = true,
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val viewConfiguration = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()
    val effectiveEdgeBlur = edgeBlurEnabled && !suppressHeavyEffects

    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }
    val itemCenters = remember { mutableMapOf<Int, Float>() }
    val itemHeights = remember { mutableMapOf<Int, Float>() }
    val overscroll = remember { Animatable(0f) }
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun beginDrag(index: Int) {
        if (dragFromIndex != null) return
        dragFromIndex = index
        dragCurrentIndex = index
        dragOffsetY = 0f
        vibrateHaptic(context)
    }

    fun updateDrag(index: Int, deltaY: Float, pointerY: Float, viewportHeight: Float) {
        dragOffsetY += deltaY
        val autoScroll = edgeAutoScrollDelta(
            pointerY = pointerY,
            viewportHeight = viewportHeight,
            threshold = with(density) { 72.dp.toPx() },
            maxStep = with(density) { 18.dp.toPx() }
        )
        if (autoScroll != 0f) {
            dragOffsetY += autoScroll
            scope.launch { listState.scrollBy(autoScroll) }
        }
        val anchorCenter = itemCenters[index] ?: pointerY
        val nextCenter = anchorCenter + dragOffsetY
        dragCurrentIndex = findNearestListIndex(
            pointerY = nextCenter,
            itemCenters = itemCenters,
            maxDistance = Float.MAX_VALUE
        ) ?: dragCurrentIndex
    }

    fun finishDrag() {
        val from = dragFromIndex
        val to = dragCurrentIndex
        if (from != null && to != null && from != to) {
            onReorder(from, to)
        }
        dragFromIndex = null
        dragCurrentIndex = null
        dragOffsetY = 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(
                    remember(listState, dragFromIndex) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (source != NestedScrollSource.Drag || dragFromIndex != null) return Offset.Zero
                                return consumeListOverscroll(available.y, listState, overscroll, scope)
                            }

                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (source != NestedScrollSource.Drag || dragFromIndex != null) return Offset.Zero
                                return consumeListOverscroll(available.y, listState, overscroll, scope)
                            }

                            override suspend fun onPreFling(available: Velocity): Velocity {
                                if (dragFromIndex != null) return Velocity.Zero
                                val atTop = !listState.canScrollBackward
                                if ((overscroll.value > 80f || available.y > 1200f) && atTop) {
                                    overscroll.snapTo(0f)
                                    onScrollToTop()
                                    return available
                                }
                                if (overscroll.value != 0f) {
                                    overscroll.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 460f))
                                    return available
                                }
                                return Velocity.Zero
                            }
                        }
                    }
                )
                .platformBlur(16f, longPressedApp != null && blurEnabled && !suppressHeavyEffects)
        ) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenCenterY = screenHeightPx / 2f
            val autoScrollSpec = remember(screenHeightPx, density) {
                DrawerEdgeAutoScrollSpec(
                    viewportHeight = screenHeightPx,
                    threshold = with(density) { 72.dp.toPx() },
                    maxStep = with(density) { 18.dp.toPx() }
                )
            }
            val scaledIconSize = iconSize * iconScaleMultiplier.coerceIn(0.8f, 1.35f)
            val estimatedItemHeight = scaledIconSize.coerceAtLeast(48.dp) + 20.dp
            val topPadding = 24.dp
            val bottomPadding = (estimatedItemHeight + 16.dp).coerceAtLeast(56.dp)
            val dragRowShift = dragFromIndex?.let { itemHeights[it] } ?: with(density) { estimatedItemHeight.toPx() }
            val touchSlop = viewConfiguration.touchSlop

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = overscroll.value }
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
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                    val itemScale = computeItemScale(itemInfo, screenCenterY, screenHeightPx)
                    val useSoftBlur = blurEnabled &&
                        effectiveEdgeBlur &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                        isNearBottom(itemInfo, screenHeightPx)
                    val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val isDragged = dragFromIndex == index
                    val displacedTarget = listDisplacementForIndex(index, dragFromIndex, dragCurrentIndex, dragRowShift)
                    val animatedDisplacement by animateFloatAsState(
                        targetValue = if (isDragged) dragOffsetY else displacedTarget,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                        label = "list_drag_displacement"
                    )
                    val pressedScale by animateFloatAsState(
                        targetValue = if (isPressed || isDragged) 0.97f else 1f,
                        animationSpec = tween(durationMillis = 170),
                        label = "list_press_scale"
                    )
                    val pressedOverlay by animateFloatAsState(
                        targetValue = if (isPressed || isDragged) 0.10f else 0f,
                        animationSpec = tween(durationMillis = 170),
                        label = "list_press_overlay"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val posY = coords.positionInRoot().y
                                itemCenters[index] = posY + coords.size.height / 2f
                                itemHeights[index] = coords.size.height.toFloat()
                            }
                            .pointerInput(app.componentKey, dragFromIndex, longPressedApp, itemCenters[index]) {
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
                                            updateDrag(index, delta.y, pointerPosition.y, screenHeightPx)
                                        },
                                        onFinishDrag = { finishDrag() }
                                    )
                                }
                            }
                            .graphicsLayer {
                                val targetScale = itemScale * pressedScale
                                translationY = animatedDisplacement
                                scaleX = targetScale
                                scaleY = targetScale
                                shadowElevation = if (isDragged) 18.dp.toPx() else 0f
                                alpha = if (isDragged) 0.96f else itemScale.coerceIn(0.3f, 1f)
                            }
                            .background(Color.Black.copy(alpha = pressedOverlay), RoundedCornerShape(18.dp))
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    val centerY = itemCenters[index] ?: screenCenterY
                                    onAppClick(app, Offset(0.15f, centerY / screenHeightPx))
                                },
                                onLongClick = null
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = if (useSoftBlur) app.cachedBlurredIcon else app.cachedIcon,
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

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
    }

    longPressedApp?.let { app ->
        AppShortcutOverlay(app = app, blurEnabled = menuBlurEnabled, onDismiss = { longPressedApp = null })
    }
}

private fun consumeListOverscroll(
    availableY: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    overscroll: Animatable<Float, AnimationVector1D>,
    scope: kotlinx.coroutines.CoroutineScope
): Offset {
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

private fun computeItemScale(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    if (itemInfo == null) return 0.85f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val dist = abs(itemCenterY - screenCenterY)
    val maxDist = screenHeight / 2f
    val t = (dist / maxDist).coerceIn(0f, 1f)
    return 1f - 0.2f * t
}

private fun isNearBottom(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenHeight: Float
): Boolean {
    if (itemInfo == null) return false
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val edgeDist = (screenHeight - itemCenterY).coerceAtLeast(0f)
    return edgeDist in 0f..(screenHeight * 0.18f)
}
