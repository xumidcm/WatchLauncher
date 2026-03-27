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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.wlauncher.R
import com.example.wlauncher.config.IconScalePreset
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.theme.WatchColors

@Composable
fun LauncherSettingsSheet(
    currentLayout: LayoutMode,
    blurEnabled: Boolean,
    edgeBlurEnabled: Boolean = false,
    lowResIcons: Boolean = false,
    animationOverrideEnabled: Boolean = true,
    splashIcon: Boolean = true,
    splashDelay: Int = 500,
    listIconSize: Int = 48,
    honeycombCols: Int = 4,
    honeycombTopBlur: Int = 12,
    honeycombBottomBlur: Int = 12,
    honeycombTopFade: Int = 56,
    honeycombBottomFade: Int = 56,
    showNotification: Boolean = true,
    appLaunchBlurEnabled: Boolean = blurEnabled,
    edgeGradientBlurEnabled: Boolean = edgeBlurEnabled,
    menuBlurEnabled: Boolean = blurEnabled,
    blurRadiusDp: Int = 4,
    appReturnAnimationDuration: Int = 220,
    iconScalePreset: String = IconScalePreset.AUTO.storageValue,
    autoIconSize: Boolean = true,
    onLayoutChange: (LayoutMode) -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onEdgeBlurToggle: (Boolean) -> Unit = {},
    onLowResToggle: (Boolean) -> Unit = {},
    onAnimationOverrideToggle: (Boolean) -> Unit = {},
    onSplashToggle: (Boolean) -> Unit = {},
    onSplashDelayChange: (Int) -> Unit = {},
    onListIconSizeChange: (Int) -> Unit = {},
    onHoneycombColsChange: (Int) -> Unit = {},
    onHoneycombTopBlurChange: (Int) -> Unit = {},
    onHoneycombBottomBlurChange: (Int) -> Unit = {},
    onHoneycombTopFadeChange: (Int) -> Unit = {},
    onHoneycombBottomFadeChange: (Int) -> Unit = {},
    onShowNotificationChange: (Boolean) -> Unit = {},
    onAppLaunchBlurToggle: (Boolean) -> Unit = {},
    onEdgeGradientBlurToggle: (Boolean) -> Unit = {},
    onMenuBlurToggle: (Boolean) -> Unit = {},
    onBlurRadiusChange: (Int) -> Unit = {},
    onAppReturnAnimationDurationChange: (Int) -> Unit = {},
    onIconScalePresetChange: (String) -> Unit = {},
    onAutoIconSizeToggle: (Boolean) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val presets = listOf(
        IconScalePreset.AUTO,
        IconScalePreset.COMPACT,
        IconScalePreset.STANDARD,
        IconScalePreset.LARGE,
        IconScalePreset.XLARGE
    )

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
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item("layout_header") {
                ScaledSectionHeader(
                    text = stringResource(R.string.settings_section_layout),
                    listState = listState,
                    key = "layout_header",
                    screenCenterY = screenCenterY,
                    screenHeight = screenHeightPx
                )
            }
            item("layout_honeycomb") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "layout_honeycomb" }, screenCenterY, screenHeightPx)
                OptionRow(
                    label = stringResource(R.string.settings_layout_honeycomb),
                    description = stringResource(R.string.settings_layout_honeycomb_desc),
                    selected = currentLayout == LayoutMode.Honeycomb,
                    onClick = { onLayoutChange(LayoutMode.Honeycomb) },
                    scale = scale
                )
            }
            item("layout_list") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "layout_list" }, screenCenterY, screenHeightPx)
                OptionRow(
                    label = stringResource(R.string.settings_layout_list),
                    description = stringResource(R.string.settings_layout_list_desc),
                    selected = currentLayout == LayoutMode.List,
                    onClick = { onLayoutChange(LayoutMode.List) },
                    scale = scale
                )
            }

            item("effects_header") {
                ScaledSectionHeader(stringResource(R.string.settings_section_effects), listState, "effects_header", screenCenterY, screenHeightPx)
            }
            item("app_blur") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "app_blur" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_blur_app_transition),
                    description = stringResource(R.string.settings_blur_app_transition_desc),
                    isOn = appLaunchBlurEnabled,
                    onToggle = onAppLaunchBlurToggle,
                    scale = scale
                )
            }
            item("edge_blur") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "edge_blur" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_blur_edge_gradient),
                    description = stringResource(R.string.settings_blur_edge_gradient_desc),
                    isOn = edgeGradientBlurEnabled,
                    onToggle = {
                        onEdgeGradientBlurToggle(it)
                        onEdgeBlurToggle(it)
                    },
                    scale = scale
                )
            }
            item("menu_blur") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "menu_blur" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_blur_menu_background),
                    description = stringResource(R.string.settings_blur_menu_background_desc),
                    isOn = menuBlurEnabled,
                    onToggle = onMenuBlurToggle,
                    scale = scale
                )
            }
            item("blur_radius") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "blur_radius" }, screenCenterY, screenHeightPx)
                SliderCard(
                    label = stringResource(R.string.settings_blur_radius),
                    value = blurRadiusDp.toFloat(),
                    valueRange = 0f..24f,
                    steps = 23,
                    scale = scale,
                    valueTextFor = { "${it.toInt()} dp" },
                    onValueCommitted = { onBlurRadiusChange(it.toInt()) }
                )
            }

            item("animation_header") {
                ScaledSectionHeader(stringResource(R.string.settings_section_animation), listState, "animation_header", screenCenterY, screenHeightPx)
            }
            item("animation_override") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "animation_override" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_animation_override),
                    description = stringResource(R.string.settings_animation_override_desc),
                    isOn = animationOverrideEnabled,
                    onToggle = onAnimationOverrideToggle,
                    scale = scale
                )
            }
            item("splash_toggle") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "splash_toggle" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_splash_icon),
                    description = stringResource(R.string.settings_splash_icon_desc),
                    isOn = splashIcon,
                    onToggle = onSplashToggle,
                    scale = scale
                )
            }
            item("return_duration") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "return_duration" }, screenCenterY, screenHeightPx)
                SliderCard(
                    label = stringResource(R.string.settings_app_return_duration),
                    value = appReturnAnimationDuration.toFloat(),
                    valueRange = 120f..1200f,
                    steps = 17,
                    scale = scale,
                    valueTextFor = { "${it.toInt()} ms" },
                    onValueCommitted = { onAppReturnAnimationDurationChange(it.toInt()) }
                )
            }

            item("icons_header") {
                ScaledSectionHeader(stringResource(R.string.settings_section_icons), listState, "icons_header", screenCenterY, screenHeightPx)
            }
            item("low_res") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "low_res" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_low_res_icons),
                    description = stringResource(R.string.settings_low_res_icons_desc),
                    isOn = lowResIcons,
                    onToggle = onLowResToggle,
                    scale = scale
                )
            }
            item("auto_icon") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "auto_icon" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_icon_auto_size),
                    description = stringResource(R.string.settings_icon_auto_size_desc),
                    isOn = autoIconSize,
                    onToggle = onAutoIconSizeToggle,
                    scale = scale
                )
            }
            presets.forEach { preset ->
                item("preset_${preset.storageValue}") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "preset_${preset.storageValue}" }, screenCenterY, screenHeightPx)
                    OptionRow(
                        label = stringResource(
                            when (preset) {
                                IconScalePreset.AUTO -> R.string.settings_icon_preset_auto
                                IconScalePreset.COMPACT -> R.string.settings_icon_preset_compact
                                IconScalePreset.STANDARD -> R.string.settings_icon_preset_standard
                                IconScalePreset.LARGE -> R.string.settings_icon_preset_large
                                IconScalePreset.XLARGE -> R.string.settings_icon_preset_xlarge
                                IconScalePreset.CUSTOM -> R.string.settings_icon_preset_standard
                            }
                        ),
                        description = if (preset == IconScalePreset.AUTO) {
                            stringResource(R.string.settings_icon_auto_size_desc)
                        } else {
                            "${preset.listIconSizeDp} dp"
                        },
                        selected = IconScalePreset.fromStorage(iconScalePreset) == preset,
                        enabled = !autoIconSize || preset == IconScalePreset.AUTO,
                        onClick = { onIconScalePresetChange(preset.storageValue) },
                        scale = scale
                    )
                }
            }
            if (currentLayout == LayoutMode.List) {
                item("list_icon_size") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "list_icon_size" }, screenCenterY, screenHeightPx)
                    SliderCard(
                        label = stringResource(R.string.settings_list_icon_size),
                        value = listIconSize.toFloat(),
                        valueRange = 40f..84f,
                        steps = 10,
                        scale = scale,
                        valueTextFor = { "${it.toInt()} dp" },
                        onValueCommitted = { onListIconSizeChange(it.toInt()) }
                    )
                }
            }

            if (currentLayout == LayoutMode.Honeycomb) {
                item("honeycomb_header") {
                    ScaledSectionHeader(stringResource(R.string.settings_section_honeycomb), listState, "honeycomb_header", screenCenterY, screenHeightPx)
                }
                item("honeycomb_cols") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_cols" }, screenCenterY, screenHeightPx)
                    SliderCard(
                        label = stringResource(R.string.settings_honeycomb_cols),
                        value = honeycombCols.toFloat(),
                        valueRange = 3f..6f,
                        steps = 2,
                        scale = scale,
                        valueTextFor = { it.toInt().toString() },
                        onValueCommitted = { onHoneycombColsChange(it.toInt()) }
                    )
                }
                item("honeycomb_top_blur") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_top_blur" }, screenCenterY, screenHeightPx)
                    SliderCard(
                        label = stringResource(R.string.settings_honeycomb_top_blur),
                        value = honeycombTopBlur.toFloat(),
                        valueRange = 0f..48f,
                        steps = 11,
                        enabled = edgeGradientBlurEnabled && blurEnabled,
                        scale = scale,
                        valueTextFor = { "${it.toInt()} dp" },
                        onValueCommitted = { onHoneycombTopBlurChange(it.toInt()) }
                    )
                }
                item("honeycomb_bottom_blur") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_bottom_blur" }, screenCenterY, screenHeightPx)
                    SliderCard(
                        label = stringResource(R.string.settings_honeycomb_bottom_blur),
                        value = honeycombBottomBlur.toFloat(),
                        valueRange = 0f..48f,
                        steps = 11,
                        enabled = edgeGradientBlurEnabled && blurEnabled,
                        scale = scale,
                        valueTextFor = { "${it.toInt()} dp" },
                        onValueCommitted = { onHoneycombBottomBlurChange(it.toInt()) }
                    )
                }
                item("honeycomb_top_fade") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_top_fade" }, screenCenterY, screenHeightPx)
                    SliderCard(
                        label = stringResource(R.string.settings_honeycomb_top_fade),
                        value = honeycombTopFade.toFloat(),
                        valueRange = 0f..160f,
                        steps = 15,
                        scale = scale,
                        valueTextFor = { "${it.toInt()} dp" },
                        onValueCommitted = { onHoneycombTopFadeChange(it.toInt()) }
                    )
                }
                item("honeycomb_bottom_fade") {
                    val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "honeycomb_bottom_fade" }, screenCenterY, screenHeightPx)
                    SliderCard(
                        label = stringResource(R.string.settings_honeycomb_bottom_fade),
                        value = honeycombBottomFade.toFloat(),
                        valueRange = 0f..160f,
                        steps = 15,
                        scale = scale,
                        valueTextFor = { "${it.toInt()} dp" },
                        onValueCommitted = { onHoneycombBottomFadeChange(it.toInt()) }
                    )
                }
            }

            item("notifications") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "notifications" }, screenCenterY, screenHeightPx)
                ToggleRow(
                    label = stringResource(R.string.settings_show_notification),
                    description = stringResource(R.string.settings_show_notification_desc),
                    isOn = showNotification,
                    onToggle = onShowNotificationChange,
                    scale = scale
                )
            }

            item("tools_header") {
                ScaledSectionHeader(stringResource(R.string.settings_section_tools), listState, "tools_header", screenCenterY, screenHeightPx)
            }
            item("export_log") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "export_log" }, screenCenterY, screenHeightPx)
                ToolButton(stringResource(R.string.settings_export_log), scale) {
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
                                context.getString(R.string.settings_export_chooser_title)
                            )
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, context.getString(R.string.settings_export_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            item("restore_defaults") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "restore_defaults" }, screenCenterY, screenHeightPx)
                ToolButton(stringResource(R.string.settings_restore_defaults), scale, onResetDefaults)
            }
            item("back") {
                val scale = itemFisheye(listState.layoutInfo.visibleItemsInfo.find { it.key == "back" }, screenCenterY, screenHeightPx)
                ToolButton(stringResource(R.string.settings_back), scale, onDismiss)
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
private fun OptionRow(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) scale.coerceIn(0.3f, 1f) else 0.45f }
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WatchColors.ActiveCyan.copy(alpha = 0.2f) else WatchColors.SurfaceGlass)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = if (enabled) Color.White else WatchColors.TextTertiary)
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, color = WatchColors.TextTertiary)
            }
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WatchColors.ActiveCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    scale: Float
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
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isOn) WatchColors.ActiveGreen else Color(0xFF555555)),
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
private fun SliderCard(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true,
    scale: Float,
    valueTextFor: (Float) -> String,
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
        Text(valueTextFor(sliderValue), fontSize = 12.sp, color = WatchColors.TextTertiary)
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
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, color = WatchColors.ActiveCyan)
    }
}
