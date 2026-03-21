package com.example.wlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.theme.WatchColors

@Composable
fun LauncherSettingsSheet(
    currentLayout: LayoutMode,
    blurEnabled: Boolean,
    lowResIcons: Boolean = false,
    splashIcon: Boolean = true,
    splashDelay: Int = 500,
    listIconSize: Int = 48,
    honeycombCols: Int = 4,
    iconPackName: String? = null,
    iconPacks: List<Pair<String?, String>> = emptyList(),
    onLayoutChange: (LayoutMode) -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onLowResToggle: (Boolean) -> Unit = {},
    onSplashToggle: (Boolean) -> Unit = {},
    onSplashDelayChange: (Int) -> Unit = {},
    onListIconSizeChange: (Int) -> Unit = {},
    onHoneycombColsChange: (Int) -> Unit = {},
    onIconPackChange: (String?) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenCenterY = screenHeightPx / 2f

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp, start = 14.dp, end = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "title") {
                val info = listState.layoutInfo.visibleItemsInfo.find { it.key == "title" }
                val s = itemFisheye(info, screenCenterY, screenHeightPx)
                Text(
                    text = "桌面设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = s }
                )
            }

            item(key = "h_layout") { ScaledSectionHeader("应用列表布局", listState, "h_layout", screenCenterY, screenHeightPx) }
            item(key = "honeycomb") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb" }, screenCenterY, screenHeightPx)
                SettingOption("蜂窝布局", "watchOS 经典圆形网格", currentLayout == LayoutMode.Honeycomb, { onLayoutChange(LayoutMode.Honeycomb) }, s)
            }
            item(key = "list") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "list" }, screenCenterY, screenHeightPx)
                SettingOption("列表布局", "按字母排序的线性列表", currentLayout == LayoutMode.List, { onLayoutChange(LayoutMode.List) }, s)
            }

            // 列表图标大小
            if (currentLayout == LayoutMode.List) {
                item(key = "list_icon_size") {
                    val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "list_icon_size" }, screenCenterY, screenHeightPx)
                    StepperSetting("图标大小", "${listIconSize}dp", listIconSize, 32, 80, 4, onListIconSizeChange, s)
                }
            }
            // 蜂窝列数
            if (currentLayout == LayoutMode.Honeycomb) {
                item(key = "honeycomb_cols") {
                    val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_cols" }, screenCenterY, screenHeightPx)
                    StepperSetting("窄行列数", "$honeycombCols (宽行 ${honeycombCols + 1})", honeycombCols, 3, 6, 1, onHoneycombColsChange, s)
                }
            }

            item(key = "h_anim") { ScaledSectionHeader("动画效果", listState, "h_anim", screenCenterY, screenHeightPx) }
            item(key = "blur") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "blur" }, screenCenterY, screenHeightPx)
                SettingToggle("模糊动画", "需要 Android 12+ (API 31)", blurEnabled, onBlurToggle, s)
            }
            item(key = "splash") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash" }, screenCenterY, screenHeightPx)
                SettingToggle("启动遮罩", "打开应用时显示居中图标", splashIcon, onSplashToggle, s)
            }
            if (splashIcon) {
                item(key = "splash_delay") {
                    val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash_delay" }, screenCenterY, screenHeightPx)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .graphicsLayer { scaleX = s; scaleY = s; alpha = s }
                            .clip(RoundedCornerShape(16.dp))
                            .background(WatchColors.SurfaceGlass)
                            .padding(14.dp)
                    ) {
                        Text("启动延迟: ${splashDelay}ms", fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Slider(
                            value = splashDelay.toFloat(),
                            onValueChange = { onSplashDelayChange(it.toInt()) },
                            valueRange = 300f..1500f,
                            steps = 11,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = WatchColors.ActiveCyan,
                                activeTrackColor = WatchColors.ActiveCyan
                            )
                        )
                    }
                }
            }

            item(key = "h_perf") { ScaledSectionHeader("性能", listState, "h_perf", screenCenterY, screenHeightPx) }
            item(key = "lowres") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "lowres" }, screenCenterY, screenHeightPx)
                SettingToggle("低分辨率图标", "降低图标质量以提升滚动流畅度", lowResIcons, onLowResToggle, s)
            }

            // 图标包
            item(key = "h_iconpack") { ScaledSectionHeader("图标包", listState, "h_iconpack", screenCenterY, screenHeightPx) }
            item(key = "iconpack_default") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "iconpack_default" }, screenCenterY, screenHeightPx)
                SettingOption("默认图标", "使用系统原生图标", iconPackName == null, { onIconPackChange(null) }, s)
            }
            for ((idx, pack) in iconPacks.withIndex()) {
                item(key = "iconpack_$idx") {
                    val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "iconpack_$idx" }, screenCenterY, screenHeightPx)
                    SettingOption(pack.second, pack.first ?: "", iconPackName == pack.first, { onIconPackChange(pack.first) }, s)
                }
            }

            item(key = "h_about") { ScaledSectionHeader("关于", listState, "h_about", screenCenterY, screenHeightPx) }
            item(key = "about") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "about" }, screenCenterY, screenHeightPx)
                Row(
                    modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s; alpha = s }
                        .clip(RoundedCornerShape(16.dp)).background(WatchColors.SurfaceGlass).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WatchLauncher", fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("v1.0 · watchOS 26 Style", fontSize = 12.sp, color = WatchColors.TextTertiary)
                    }
                }
            }

            item(key = "back") {
                val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "back" }, screenCenterY, screenHeightPx)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s; alpha = s }
                        .clip(RoundedCornerShape(16.dp)).background(WatchColors.SurfaceGlass)
                        .clickable { onDismiss() }.padding(14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("返回", fontSize = 14.sp, color = WatchColors.ActiveCyan) }
            }
        }
    }
}

private fun itemFisheye(
    info: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    if (info == null) return 0.9f
    val itemCenterY = info.offset + info.size / 2f
    // 只在底部有鱼眼缩放效果，上部保持1.0
    if (itemCenterY <= screenCenterY) return 1f
    val dist = itemCenterY - screenCenterY
    val maxDist = screenHeight / 2f
    val t = (dist / maxDist).coerceIn(0f, 1f)
    return 1f - 0.15f * t
}

@Composable
private fun ScaledSectionHeader(
    text: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    key: String,
    screenCenterY: Float,
    screenHeight: Float
) {
    val s = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == key }, screenCenterY, screenHeight)
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = WatchColors.TextTertiary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
            .graphicsLayer { scaleX = s; scaleY = s; alpha = s }
    )
}

@Composable
private fun SettingOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    scale: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale.coerceIn(0.3f, 1f) }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) WatchColors.ActiveCyan.copy(alpha = 0.2f) else WatchColors.SurfaceGlass)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 12.sp, color = WatchColors.TextTertiary)
        }
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WatchColors.ActiveCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    description: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    scale: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale.coerceIn(0.3f, 1f) }
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable { onToggle(!isOn) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 12.sp, color = WatchColors.TextTertiary)
        }
        Box(
            modifier = Modifier.width(44.dp).height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isOn) WatchColors.ActiveGreen else Color(0xFF555555)),
            contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(modifier = Modifier.padding(2.dp).size(20.dp).clip(RoundedCornerShape(10.dp)).background(Color.White))
        }
    }
}

@Composable
private fun StepperSetting(
    label: String,
    valueText: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onChange: (Int) -> Unit,
    scale: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale.coerceIn(0.3f, 1f) }
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(valueText, fontSize = 12.sp, color = WatchColors.TextTertiary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (value > min) WatchColors.ActiveCyan.copy(alpha = 0.3f) else Color(0xFF333333))
                    .clickable(enabled = value > min) { onChange((value - step).coerceAtLeast(min)) },
                contentAlignment = Alignment.Center
            ) { Text("−", color = if (value > min) WatchColors.ActiveCyan else Color(0xFF666666), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (value < max) WatchColors.ActiveCyan.copy(alpha = 0.3f) else Color(0xFF333333))
                    .clickable(enabled = value < max) { onChange((value + step).coerceAtMost(max)) },
                contentAlignment = Alignment.Center
            ) { Text("+", color = if (value < max) WatchColors.ActiveCyan else Color(0xFF666666), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
