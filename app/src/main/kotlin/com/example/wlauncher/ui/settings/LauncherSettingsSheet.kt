package com.example.wlauncher.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.theme.WatchColors

@Composable
fun LauncherSettingsSheet(
    currentLayout: LayoutMode,
    blurEnabled: Boolean,
    edgeBlurEnabled: Boolean = false,
    lowResIcons: Boolean = false,
    splashIcon: Boolean = true,
    splashDelay: Int = 500,
    listIconSize: Int = 48,
    honeycombCols: Int = 4,
    honeycombTopBlur: Int = 12,
    honeycombBottomBlur: Int = 12,
    honeycombTopFade: Int = 56,
    honeycombBottomFade: Int = 56,
    showNotification: Boolean = true,
    onLayoutChange: (LayoutMode) -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onEdgeBlurToggle: (Boolean) -> Unit = {},
    onLowResToggle: (Boolean) -> Unit = {},
    onSplashToggle: (Boolean) -> Unit = {},
    onSplashDelayChange: (Int) -> Unit = {},
    onListIconSizeChange: (Int) -> Unit = {},
    onHoneycombColsChange: (Int) -> Unit = {},
    onHoneycombTopBlurChange: (Int) -> Unit = {},
    onHoneycombBottomBlurChange: (Int) -> Unit = {},
    onHoneycombTopFadeChange: (Int) -> Unit = {},
    onHoneycombBottomFadeChange: (Int) -> Unit = {},
    onShowNotificationChange: (Boolean) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val isZh = rememberSystemIsChinese()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenCenterY = screenHeightPx / 2f

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp, start = 14.dp, end = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item("title") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "title" }, screenCenterY, screenHeightPx)
                Text(
                    text = tr(isZh, "桌面设置", "Launcher Settings"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale }
                )
            }

            item("layout_header") { ScaledSectionHeader(tr(isZh, "布局", "Layout"), listState, "layout_header", screenCenterY, screenHeightPx) }
            item("layout_honeycomb") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "layout_honeycomb" }, screenCenterY, screenHeightPx)
                SettingOption(tr(isZh, "蜂窝布局", "Honeycomb"), tr(isZh, "Apple Watch 风格网格", "Apple Watch style grid"), currentLayout == LayoutMode.Honeycomb, { onLayoutChange(LayoutMode.Honeycomb) }, scale)
            }
            item("layout_list") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "layout_list" }, screenCenterY, screenHeightPx)
                SettingOption(tr(isZh, "列表布局", "List"), tr(isZh, "纵向应用列表", "Vertical app list"), currentLayout == LayoutMode.List, { onLayoutChange(LayoutMode.List) }, scale)
            }

            item("effects_header") { ScaledSectionHeader(tr(isZh, "效果", "Effects"), listState, "effects_header", screenCenterY, screenHeightPx) }
            item("blur_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "blur_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(tr(isZh, "模糊", "Blur"), tr(isZh, "在支持的 Android 版本上启用模糊", "Enable blur on supported Android versions"), blurEnabled, onBlurToggle, scale = scale)
            }
            item("edge_blur_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "edge_blur_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(
                    label = tr(isZh, "边缘模糊（实验）", "Edge Blur (Experimental)"),
                    description = if (blurEnabled) tr(isZh, "在顶部和底部边缘增加模糊", "Apply extra blur near the top and bottom edges") else tr(isZh, "请先开启模糊", "Enable Blur first"),
                    isOn = edgeBlurEnabled,
                    onToggle = onEdgeBlurToggle,
                    enabled = blurEnabled,
                    scale = scale
                )
            }
            item("splash_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(tr(isZh, "启动遮罩", "Launch Overlay"), tr(isZh, "启动应用时显示居中图标", "Show the centered app icon while launching"), splashIcon, onSplashToggle, scale = scale)
            }
            if (splashIcon) {
                item("splash_delay") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash_delay" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "遮罩延迟", "Overlay Delay"),
                        valueText = "$splashDelay ms",
                        value = splashDelay.toFloat(),
                        valueRange = 300f..1500f,
                        steps = 11,
                        scale = scale,
                        onValueCommitted = { onSplashDelayChange(it.toInt()) }
                    )
                }
            }

            item("performance_header") { ScaledSectionHeader(tr(isZh, "性能", "Performance"), listState, "performance_header", screenCenterY, screenHeightPx) }
            item("low_res_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "low_res_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(tr(isZh, "低分辨率图标", "Low-res Icons"), tr(isZh, "使用更小的缓存图标来提升滚动流畅度", "Use smaller cached icons for smoother scrolling"), lowResIcons, onLowResToggle, scale = scale)
            }

            if (currentLayout == LayoutMode.List) {
                item("list_icon_size") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "list_icon_size" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "列表图标大小", "List Icon Size"),
                        valueText = "$listIconSize dp",
                        value = listIconSize.toFloat(),
                        valueRange = 40f..84f,
                        steps = 10,
                        scale = scale,
                        onValueCommitted = { onListIconSizeChange(it.toInt()) }
                    )
                }
            }

            if (currentLayout == LayoutMode.Honeycomb) {
                item("honeycomb_header") { ScaledSectionHeader(tr(isZh, "蜂窝边缘调节", "Honeycomb Edge Tuning"), listState, "honeycomb_header", screenCenterY, screenHeightPx) }
                item("honeycomb_cols") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_cols" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "窄行列数", "Narrow Row Columns"),
                        valueText = honeycombCols.toString(),
                        value = honeycombCols.toFloat(),
                        valueRange = 3f..6f,
                        steps = 2,
                        scale = scale,
                        onValueCommitted = { onHoneycombColsChange(it.toInt()) }
                    )
                }
                item("honeycomb_top_blur") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_top_blur" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "顶部模糊半径", "Top Blur Radius"),
                        valueText = "$honeycombTopBlur dp",
                        value = honeycombTopBlur.toFloat(),
                        valueRange = 0f..48f,
                        steps = 11,
                        enabled = edgeBlurEnabled && blurEnabled,
                        scale = scale,
                        onValueCommitted = { onHoneycombTopBlurChange(it.toInt()) }
                    )
                }
                item("honeycomb_bottom_blur") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_bottom_blur" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "底部模糊半径", "Bottom Blur Radius"),
                        valueText = "$honeycombBottomBlur dp",
                        value = honeycombBottomBlur.toFloat(),
                        valueRange = 0f..48f,
                        steps = 11,
                        enabled = edgeBlurEnabled && blurEnabled,
                        scale = scale,
                        onValueCommitted = { onHoneycombBottomBlurChange(it.toInt()) }
                    )
                }
                item("honeycomb_top_fade") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_top_fade" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "顶部渐隐范围", "Top Fade Range"),
                        valueText = "$honeycombTopFade dp",
                        value = honeycombTopFade.toFloat(),
                        valueRange = 0f..160f,
                        steps = 15,
                        scale = scale,
                        onValueCommitted = { onHoneycombTopFadeChange(it.toInt()) }
                    )
                }
                item("honeycomb_bottom_fade") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_bottom_fade" }, screenCenterY, screenHeightPx)
                    DeferredSliderCard(
                        label = tr(isZh, "底部渐隐范围", "Bottom Fade Range"),
                        valueText = "$honeycombBottomFade dp",
                        value = honeycombBottomFade.toFloat(),
                        valueRange = 0f..160f,
                        steps = 15,
                        scale = scale,
                        onValueCommitted = { onHoneycombBottomFadeChange(it.toInt()) }
                    )
                }
            }

            item("tools_header") { ScaledSectionHeader(tr(isZh, "工具", "Tools"), listState, "tools_header", screenCenterY, screenHeightPx) }
            item("export_log") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "export_log" }, screenCenterY, screenHeightPx)
                ToolButton(tr(isZh, "导出日志", "Export Log"), scale) {
                    try {
                        val log = Runtime.getRuntime().exec("logcat -d -t 500").inputStream.bufferedReader().readText()
                        val file = java.io.File(context.cacheDir, "wlauncher_log.txt")
                        file.writeText(log)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                tr(isZh, "导出日志", "Export Log")
                            )
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, tr(isZh, "导出失败", "Export failed"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            item("reset_defaults") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "reset_defaults" }, screenCenterY, screenHeightPx)
                ToolButton(tr(isZh, "恢复默认设置", "Restore Defaults"), scale, onResetDefaults)
            }
            item("back") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "back" }, screenCenterY, screenHeightPx)
                ToolButton(tr(isZh, "返回", "Back"), scale, onDismiss)
            }
        }
    }
}

@Composable
private fun rememberSystemIsChinese(): Boolean {
    val context = LocalContext.current
    return remember(context.resources.configuration) {
        context.resources.configuration.locales[0]?.language?.startsWith("zh") == true
    }
}

private fun tr(isZh: Boolean, zh: String, en: String): String = if (isZh) zh else en

private fun itemFisheye(
    info: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    if (info == null) return 0.9f
    val itemCenterY = info.offset + info.size / 2f
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
    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == key }, screenCenterY, screenHeight)
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = WatchColors.TextTertiary,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale }
    )
}

@Composable
private fun SettingOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    scale: Float
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
    enabled: Boolean = true,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) scale.coerceIn(0.3f, 1f) else 0.45f }
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable(enabled = enabled) { onToggle(!isOn) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 12.sp, color = WatchColors.TextTertiary)
        }
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        !enabled -> Color(0xFF333333)
                        isOn -> WatchColors.ActiveGreen
                        else -> Color(0xFF555555)
                    }
                ),
            contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun DeferredSliderCard(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true,
    scale: Float,
    onValueCommitted: (Float) -> Unit
) {
    var sliderValue by remember(label) { mutableFloatStateOf(value) }
    LaunchedEffect(value) { sliderValue = value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) scale.coerceIn(0.3f, 1f) else 0.45f }
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .padding(14.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(valueText, fontSize = 12.sp, color = WatchColors.TextTertiary)
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueCommitted(sliderValue) },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = WatchColors.ActiveCyan,
                activeTrackColor = WatchColors.ActiveCyan
            )
        )
    }
}

@Composable
private fun ToolButton(label: String, scale: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale.coerceIn(0.3f, 1f) }
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, color = WatchColors.ActiveCyan)
    }
}
