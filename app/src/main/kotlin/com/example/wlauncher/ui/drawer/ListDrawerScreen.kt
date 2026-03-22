package com.example.wlauncher.ui.drawer

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    iconSize: Dp = 56.dp,
    textSizeSp: Int = 18,
    onAppClick: (AppInfo, Offset) -> Unit,
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val context = LocalContext.current
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }
    val itemPositions = remember { mutableMapOf<Int, Float>() }
    val overscroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                if (available.y > 0f && atTop) {
                    scope.launch {
                        overscroll.snapTo((overscroll.value + available.y * 0.45f).coerceAtMost(240f))
                    }
                    return Offset(0f, available.y)
                }
                if (overscroll.value > 0f && available.y < 0f) {
                    scope.launch {
                        overscroll.snapTo((overscroll.value + available.y).coerceAtLeast(0f))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscroll.value > 96f || available.y > 1200f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    overscroll.snapTo(0f)
                    onScrollToTop()
                    return available
                }
                if (overscroll.value > 0f) {
                    overscroll.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 420f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .platformBlur(16f, longPressedApp != null && blurEnabled)
        ) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenCenterY = screenHeightPx / 2f

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = overscroll.value }
                    .background(Color.Black),
                contentPadding = PaddingValues(top = 28.dp, bottom = 72.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(apps, key = { _, app -> "${app.packageName}/${app.activityName}" }) { index, app ->
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                    val itemScale = computeItemScale(itemInfo, screenCenterY, screenHeightPx)
                    val edgeBlur = computeBottomEdgeBlur(itemInfo, screenHeightPx)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    val posY = coords.positionInRoot().y
                                    itemPositions[index] = posY + coords.size.height / 2f
                                }
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                    alpha = itemScale.coerceIn(0.36f, 1f)
                                }
                                .platformBlur(edgeBlur, blurEnabled && edgeBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                .combinedClickable(
                                    onClick = {
                                        val centerY = itemPositions[index] ?: screenCenterY
                                        onAppClick(app, Offset(0.28f, centerY / screenHeightPx))
                                    },
                                    onLongClick = {
                                        vibrateHaptic(context)
                                        onLongClick(app)
                                        longPressedApp = app
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && blurEnabled && edgeBlurEnabled && edgeBlur > 0.5f) {
                                    app.cachedBlurredIcon
                                } else {
                                    app.cachedIcon
                                },
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(iconSize)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                fontSize = textSizeSp.sp,
                                fontWeight = FontWeight.W600,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(88.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
    }

    longPressedApp?.let { app ->
        AppShortcutOverlay(app = app, blurEnabled = blurEnabled, onDismiss = { longPressedApp = null })
    }
}

private fun computeItemScale(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    if (itemInfo == null) return 0.9f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val dist = abs(itemCenterY - screenCenterY)
    val maxDist = screenHeight / 2f
    val t = (dist / maxDist).coerceIn(0f, 1f)
    return 1f - 0.12f * t
}

private fun computeBottomEdgeBlur(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenHeight: Float
): Float {
    if (itemInfo == null) return 0f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val edgeDist = (screenHeight - itemCenterY).coerceAtLeast(0f)
    val blurZone = screenHeight * 0.22f
    if (edgeDist >= blurZone || edgeDist <= 0f) return 0f
    return ((1f - edgeDist / blurZone) * 16f).coerceIn(0f, 16f)
}
