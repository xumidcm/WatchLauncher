package com.example.wlauncher.ui.settings

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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

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
            item(key = "title") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "title" }, screenCenterY, screenHeightPx)
                Text(
                    text = "Launcher Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale }
                )
            }

            item("layout_header") { ScaledSectionHeader("Layout", listState, "layout_header", screenCenterY, screenHeightPx) }
            item("layout_honeycomb") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "layout_honeycomb" }, screenCenterY, screenHeightPx)
                SettingOption(
                    label = "Honeycomb",
                    description = "Apple Watch style grid",
                    isSelected = currentLayout == LayoutMode.Honeycomb,
                    onClick = { onLayoutChange(LayoutMode.Honeycomb) },
                    scale = scale
                )
            }
            item("layout_list") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "layout_list" }, screenCenterY, screenHeightPx)
                SettingOption(
                    label = "List",
                    description = "Vertical app list",
                    isSelected = currentLayout == LayoutMode.List,
                    onClick = { onLayoutChange(LayoutMode.List) },
                    scale = scale
                )
            }

            item("anim_header") { ScaledSectionHeader("Effects", listState, "anim_header", screenCenterY, screenHeightPx) }
            item("blur_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "blur_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(
                    label = "Blur",
                    description = "Enable blur on all supported Android versions",
                    isOn = blurEnabled,
                    onToggle = onBlurToggle,
                    scale = scale
                )
            }
            item("edge_blur_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "edge_blur_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(
                    label = "Edge Blur (Experimental)",
                    description = if (blurEnabled) "Apply extra blur near top and bottom edges" else "Enable Blur first",
                    isOn = edgeBlurEnabled,
                    onToggle = onEdgeBlurToggle,
                    enabled = blurEnabled,
                    scale = scale
                )
            }
            item("splash_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(
                    label = "Launch Overlay",
                    description = "Show the centered app icon while launching",
                    isOn = splashIcon,
                    onToggle = onSplashToggle,
                    scale = scale
                )
            }
            if (splashIcon) {
                item("splash_delay") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash_delay" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "Overlay Delay",
                        valueText = "${splashDelay} ms",
                        value = splashDelay.toFloat(),
                        valueRange = 300f..1500f,
                        steps = 11,
                        onValueChange = { onSplashDelayChange(it.toInt()) },
                        scale = scale
                    )
                }
            }

            item("perf_header") { ScaledSectionHeader("Performance", listState, "perf_header", screenCenterY, screenHeightPx) }
            item("low_res_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "low_res_toggle" }, screenCenterY, screenHeightPx)
                SettingToggle(
                    label = "Low-res Icons",
                    description = "Use smaller cached icons for smoother scrolling",
                    isOn = lowResIcons,
                    onToggle = onLowResToggle,
                    scale = scale
                )
            }

            if (currentLayout == LayoutMode.List) {
                item("list_size") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "list_size" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "List Icon Size",
                        valueText = "${listIconSize} dp",
                        value = listIconSize.toFloat(),
                        valueRange = 32f..80f,
                        steps = 11,
                        onValueChange = { onListIconSizeChange(it.toInt()) },
                        scale = scale
                    )
                }
            }

            if (currentLayout == LayoutMode.Honeycomb) {
                item("honeycomb_header") {
                    ScaledSectionHeader("Honeycomb Edge Tuning", listState, "honeycomb_header", screenCenterY, screenHeightPx)
                }
                item("honeycomb_cols") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_cols" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "Narrow Row Columns",
                        valueText = honeycombCols.toString(),
                        value = honeycombCols.toFloat(),
                        valueRange = 3f..6f,
                        steps = 2,
                        onValueChange = { onHoneycombColsChange(it.toInt()) },
                        scale = scale
                    )
                }
                item("honeycomb_top_blur") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_top_blur" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "Top Blur Radius",
                        valueText = "${honeycombTopBlur} dp",
                        value = honeycombTopBlur.toFloat(),
                        valueRange = 0f..48f,
                        steps = 11,
                        onValueChange = { onHoneycombTopBlurChange(it.toInt()) },
                        enabled = edgeBlurEnabled && blurEnabled,
                        scale = scale
                    )
                }
                item("honeycomb_bottom_blur") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_bottom_blur" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "Bottom Blur Radius",
                        valueText = "${honeycombBottomBlur} dp",
                        value = honeycombBottomBlur.toFloat(),
                        valueRange = 0f..48f,
                        steps = 11,
                        onValueChange = { onHoneycombBottomBlurChange(it.toInt()) },
                        enabled = edgeBlurEnabled && blurEnabled,
                        scale = scale
                    )
                }
                item("honeycomb_top_fade") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_top_fade" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "Top Fade Range",
                        valueText = "${honeycombTopFade} dp",
                        value = honeycombTopFade.toFloat(),
                        valueRange = 0f..160f,
                        steps = 15,
                        onValueChange = { onHoneycombTopFadeChange(it.toInt()) },
                        scale = scale
                    )
                }
                item("honeycomb_bottom_fade") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_bottom_fade" }, screenCenterY, screenHeightPx)
                    SettingSlider(
                        label = "Bottom Fade Range",
                        valueText = "${honeycombBottomFade} dp",
                        value = honeycombBottomFade.toFloat(),
                        valueRange = 0f..160f,
                        steps = 15,
                        onValueChange = { onHoneycombBottomFadeChange(it.toInt()) },
                        scale = scale
                    )
                }
            }

            item("back") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "back" }, screenCenterY, screenHeightPx)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale }
                        .clip(RoundedCornerShape(16.dp))
                        .background(WatchColors.SurfaceGlass)
                        .clickable { onDismiss() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Back", fontSize = 14.sp, color = WatchColors.ActiveCyan)
                }
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
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = WatchColors.ActiveCyan,
                modifier = Modifier.size(20.dp)
            )
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
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    scale: Float
) {
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
            value = value,
            onValueChange = onValueChange,
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
