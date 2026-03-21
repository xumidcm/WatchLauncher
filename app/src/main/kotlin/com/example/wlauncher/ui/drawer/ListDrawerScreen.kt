package com.example.wlauncher.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.theme.WatchColors
import kotlin.math.abs

@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenCenterY = screenHeightPx / 2f

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(
                    top = 40.dp,
                    bottom = 60.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "__settings__") {
                    val itemScale = computeItemScale(listState, 0, screenCenterY, screenHeightPx, density)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemScale.coerceIn(0.3f, 1f)
                            }
                            .clickable { onSettingsClick() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = WatchColors.TextSecondary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "桌面设置",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500,
                            color = WatchColors.ActiveCyan
                        )
                    }
                }

                items(apps, key = { it.packageName }) { app ->
                    val itemIndex = apps.indexOf(app) + 1 // +1 for settings item
                    val itemScale = computeItemScale(listState, itemIndex, screenCenterY, screenHeightPx, density)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemScale.coerceIn(0.3f, 1f)
                            }
                            .clickable { onAppClick(app) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = app.cachedIcon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = app.label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 顶部渐变遮罩
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
        )
        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
    }
}

/**
 * 根据 item 在屏幕上的位置计算缩放比例（靠近中心=1.0，靠近边缘缩小）
 */
private fun computeItemScale(
    listState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    screenCenterY: Float,
    screenHeight: Float,
    density: androidx.compose.ui.unit.Density
): Float {
    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
        ?: return 0.85f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val dist = abs(itemCenterY - screenCenterY)
    val maxDist = screenHeight / 2f
    val t = (dist / maxDist).coerceIn(0f, 1f)
    return 1f - 0.2f * t // 1.0 at center, 0.8 at edge
}
