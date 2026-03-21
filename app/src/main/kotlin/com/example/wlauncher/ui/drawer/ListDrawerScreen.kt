package com.example.wlauncher.ui.drawer

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    iconSize: Dp = 48.dp,
    onAppClick: (AppInfo, Offset) -> Unit,
    onLongClick: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val useBlurApi = blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }

    // 记录每个可见 item 的屏幕内实际位置
    val itemPositions = remember { mutableMapOf<Int, Float>() }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenCenterY = screenHeightPx / 2f
            // 首项居中：top padding = 屏幕中心 - item高度/2
            val itemHeightApprox = with(density) { (iconSize + 20.dp).toPx() }
            val topPadding = with(density) { ((screenCenterY - itemHeightApprox / 2f) / density.density).dp }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentPadding = PaddingValues(
                    top = topPadding.coerceAtLeast(20.dp),
                    bottom = topPadding.coerceAtLeast(60.dp),
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(apps, key = { _, app -> "${app.packageName}/${app.activityName}" }) { index, app ->
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                    val itemScale = computeItemScale(itemInfo, screenCenterY, screenHeightPx)
                    val edgeBlur = computeEdgeBlur(itemInfo, screenHeightPx, density)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val posY = coords.positionInRoot().y
                                val centerY = posY + coords.size.height / 2f
                                itemPositions[index] = centerY
                            }
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemScale.coerceIn(0.3f, 1f)
                                if (useBlurApi && edgeBlur > 0.5f) {
                                    renderEffect = RenderEffect.createBlurEffect(
                                        edgeBlur, edgeBlur, Shader.TileMode.CLAMP
                                    ).asComposeRenderEffect()
                                }
                            }
                            .combinedClickable(
                                onClick = {
                                    val centerY = itemPositions[index] ?: screenCenterY
                                    onAppClick(app, Offset(0.15f, centerY / screenHeightPx))
                                },
                                onLongClick = {
                                    vibrateHaptic(context)
                                    longPressedApp = app
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = app.cachedIcon,
                            contentDescription = app.label,
                            modifier = Modifier.size(iconSize).clip(CircleShape),
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

        // 顶部渐变遮罩
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(60.dp)
                .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
        )
        // 底部渐变遮罩
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
    }

    // App Shortcuts 弹窗
    longPressedApp?.let { app ->
        AppShortcutPopup(app = app, onDismiss = { longPressedApp = null })
    }
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
    return 1f - 0.15f * t
}

private fun computeEdgeBlur(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenHeight: Float,
    density: androidx.compose.ui.unit.Density
): Float {
    if (itemInfo == null) return 0f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val edgeDist = minOf(itemCenterY.coerceAtLeast(0f), (screenHeight - itemCenterY).coerceAtLeast(0f))
    val blurZone = screenHeight * 0.18f
    if (edgeDist >= blurZone) return 0f
    return ((1f - edgeDist / blurZone) * 10f * density.density).coerceIn(0f, 10f * density.density)
}
