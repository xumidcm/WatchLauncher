package com.example.wlauncher.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
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
    appOpenAnimationDuration: Int = 280,
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
    onAppOpenAnimationDurationChange: (Int) -> Unit = {},
    onAppReturnAnimationDurationChange: (Int) -> Unit = {},
    onIconScalePresetChange: (String) -> Unit = {},
    onAutoIconSizeToggle: (Boolean) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presets = listOf(
        IconScalePreset.AUTO,
        IconScalePreset.COMPACT,
        IconScalePreset.STANDARD,
        IconScalePreset.LARGE,
        IconScalePreset.XLARGE
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(14.dp),
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

        item("layout") {
            SectionTitle(stringResource(R.string.settings_section_layout))
            OptionRow(
                label = stringResource(R.string.settings_layout_honeycomb),
                description = stringResource(R.string.settings_layout_honeycomb_desc),
                selected = currentLayout == LayoutMode.Honeycomb,
                onClick = { onLayoutChange(LayoutMode.Honeycomb) }
            )
            OptionRow(
                label = stringResource(R.string.settings_layout_list),
                description = stringResource(R.string.settings_layout_list_desc),
                selected = currentLayout == LayoutMode.List,
                onClick = { onLayoutChange(LayoutMode.List) }
            )
        }

        item("effects") {
            SectionTitle(stringResource(R.string.settings_section_effects))
            ToggleRow(
                label = stringResource(R.string.settings_blur_master),
                description = stringResource(R.string.settings_blur_master_desc),
                isOn = blurEnabled,
                onToggle = onBlurToggle
            )
            ToggleRow(
                label = stringResource(R.string.settings_blur_app_transition),
                description = stringResource(R.string.settings_blur_app_transition_desc),
                isOn = appLaunchBlurEnabled,
                onToggle = onAppLaunchBlurToggle
            )
            ToggleRow(
                label = stringResource(R.string.settings_blur_edge_gradient),
                description = stringResource(R.string.settings_blur_edge_gradient_desc),
                isOn = edgeGradientBlurEnabled,
                onToggle = {
                    onEdgeGradientBlurToggle(it)
                    onEdgeBlurToggle(it)
                }
            )
            ToggleRow(
                label = stringResource(R.string.settings_blur_menu_background),
                description = stringResource(R.string.settings_blur_menu_background_desc),
                isOn = menuBlurEnabled,
                onToggle = onMenuBlurToggle
            )
            SliderCard(
                label = stringResource(R.string.settings_blur_radius),
                valueText = stringResource(R.string.settings_value_dp, blurRadiusDp),
                value = blurRadiusDp.toFloat(),
                valueRange = 0f..24f,
                steps = 23,
                onValueCommitted = { onBlurRadiusChange(it.toInt()) }
            )
        }

        item("animation") {
            SectionTitle(stringResource(R.string.settings_section_animation))
            ToggleRow(
                label = stringResource(R.string.settings_animation_override),
                description = stringResource(R.string.settings_animation_override_desc),
                isOn = animationOverrideEnabled,
                onToggle = onAnimationOverrideToggle
            )
            ToggleRow(
                label = stringResource(R.string.settings_splash_icon),
                description = stringResource(R.string.settings_splash_icon_desc),
                isOn = splashIcon,
                onToggle = onSplashToggle
            )
            SliderCard(
                label = stringResource(R.string.settings_splash_delay),
                valueText = stringResource(R.string.settings_value_ms, splashDelay),
                value = splashDelay.toFloat(),
                valueRange = 300f..1500f,
                steps = 11,
                enabled = splashIcon,
                onValueCommitted = { onSplashDelayChange(it.toInt()) }
            )
            SliderCard(
                label = stringResource(R.string.settings_app_open_duration),
                valueText = stringResource(R.string.settings_value_ms, appOpenAnimationDuration),
                value = appOpenAnimationDuration.toFloat(),
                valueRange = 120f..1200f,
                steps = 17,
                onValueCommitted = { onAppOpenAnimationDurationChange(it.toInt()) }
            )
            SliderCard(
                label = stringResource(R.string.settings_app_return_duration),
                valueText = stringResource(R.string.settings_value_ms, appReturnAnimationDuration),
                value = appReturnAnimationDuration.toFloat(),
                valueRange = 120f..1200f,
                steps = 17,
                onValueCommitted = { onAppReturnAnimationDurationChange(it.toInt()) }
            )
        }

        item("icons") {
            SectionTitle(stringResource(R.string.settings_section_icons))
            ToggleRow(
                label = stringResource(R.string.settings_low_res_icons),
                description = stringResource(R.string.settings_low_res_icons_desc),
                isOn = lowResIcons,
                onToggle = onLowResToggle
            )
            ToggleRow(
                label = stringResource(R.string.settings_icon_auto_size),
                description = stringResource(R.string.settings_icon_auto_size_desc),
                isOn = autoIconSize,
                onToggle = onAutoIconSizeToggle
            )
            presets.forEach { preset ->
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
                        stringResource(R.string.settings_value_dp, preset.listIconSizeDp)
                    },
                    selected = IconScalePreset.fromStorage(iconScalePreset) == preset,
                    enabled = !autoIconSize || preset == IconScalePreset.AUTO,
                    onClick = { onIconScalePresetChange(preset.storageValue) }
                )
            }
            if (currentLayout == LayoutMode.List) {
                SliderCard(
                    label = stringResource(R.string.settings_list_icon_size),
                    valueText = stringResource(R.string.settings_value_dp, listIconSize),
                    value = listIconSize.toFloat(),
                    valueRange = 40f..84f,
                    steps = 10,
                    onValueCommitted = { onListIconSizeChange(it.toInt()) }
                )
            }
        }

        if (currentLayout == LayoutMode.Honeycomb) {
            item("honeycomb") {
                SectionTitle(stringResource(R.string.settings_section_honeycomb))
                SliderCard(
                    label = stringResource(R.string.settings_honeycomb_cols),
                    valueText = honeycombCols.toString(),
                    value = honeycombCols.toFloat(),
                    valueRange = 3f..6f,
                    steps = 2,
                    onValueCommitted = { onHoneycombColsChange(it.toInt()) }
                )
                SliderCard(
                    label = stringResource(R.string.settings_honeycomb_top_blur),
                    valueText = stringResource(R.string.settings_value_dp, honeycombTopBlur),
                    value = honeycombTopBlur.toFloat(),
                    valueRange = 0f..48f,
                    steps = 11,
                    enabled = edgeGradientBlurEnabled && blurEnabled,
                    onValueCommitted = { onHoneycombTopBlurChange(it.toInt()) }
                )
                SliderCard(
                    label = stringResource(R.string.settings_honeycomb_bottom_blur),
                    valueText = stringResource(R.string.settings_value_dp, honeycombBottomBlur),
                    value = honeycombBottomBlur.toFloat(),
                    valueRange = 0f..48f,
                    steps = 11,
                    enabled = edgeGradientBlurEnabled && blurEnabled,
                    onValueCommitted = { onHoneycombBottomBlurChange(it.toInt()) }
                )
                SliderCard(
                    label = stringResource(R.string.settings_honeycomb_top_fade),
                    valueText = stringResource(R.string.settings_value_dp, honeycombTopFade),
                    value = honeycombTopFade.toFloat(),
                    valueRange = 0f..160f,
                    steps = 15,
                    onValueCommitted = { onHoneycombTopFadeChange(it.toInt()) }
                )
                SliderCard(
                    label = stringResource(R.string.settings_honeycomb_bottom_fade),
                    valueText = stringResource(R.string.settings_value_dp, honeycombBottomFade),
                    value = honeycombBottomFade.toFloat(),
                    valueRange = 0f..160f,
                    steps = 15,
                    onValueCommitted = { onHoneycombBottomFadeChange(it.toInt()) }
                )
            }
        }

        item("notifications") {
            ToggleRow(
                label = stringResource(R.string.settings_show_notification),
                description = stringResource(R.string.settings_show_notification_desc),
                isOn = showNotification,
                onToggle = onShowNotificationChange
            )
        }

        item("tools") {
            SectionTitle(stringResource(R.string.settings_section_tools))
            ToolButton(stringResource(R.string.settings_export_log)) {
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
            ToolButton(stringResource(R.string.settings_restore_defaults), onResetDefaults)
            ToolButton(stringResource(R.string.settings_back), onDismiss)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = WatchColors.TextTertiary
    )
}

@Composable
private fun OptionRow(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WatchColors.ActiveCyan.copy(alpha = 0.2f) else WatchColors.SurfaceGlass)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                color = if (enabled) Color.White else WatchColors.TextTertiary
            )
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
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true,
    onValueCommitted: (Float) -> Unit
) {
    var sliderValue by remember(label) { mutableFloatStateOf(value) }
    LaunchedEffect(value) { sliderValue = value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
private fun ToolButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, color = WatchColors.ActiveCyan)
    }
}
