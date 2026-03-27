package com.example.wlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import com.example.wlauncher.config.IconScalePreset
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.settings.LauncherSettingsSheet
import com.example.wlauncher.ui.theme.WatchLauncherTheme
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_ANIMATION_OVERRIDE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_APP_RETURN_ANIM_DURATION
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_APP_TRANSITION_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_BLUR_RADIUS
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_EDGE_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_EDGE_GRADIENT_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_BOTTOM_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_BOTTOM_FADE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_COLS
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_FISHEYE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_TOP_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_TOP_FADE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_ICON_SIZE_AUTO
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_ICON_SIZE_PRESET
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_LAYOUT
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_LIST_ICON_SIZE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_LOW_RES
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_MENU_BG_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_SHOW_NOTIFICATION
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_SPLASH_DELAY
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_SPLASH_ICON
import com.example.wlauncher.viewmodel.dataStore
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchLauncherTheme {
                val scope = rememberCoroutineScope()
                val prefs by dataStore.data.collectAsState(initial = null)

                val currentLayout = prefs?.get(KEY_LAYOUT)?.let {
                    try {
                        LayoutMode.valueOf(it)
                    } catch (_: Exception) {
                        LayoutMode.Honeycomb
                    }
                } ?: LayoutMode.Honeycomb

                val appLaunchBlurEnabled = prefs?.get(KEY_APP_TRANSITION_BLUR) ?: prefs?.get(KEY_BLUR) ?: true
                val edgeGradientBlurEnabled = prefs?.get(KEY_EDGE_GRADIENT_BLUR) ?: prefs?.get(KEY_EDGE_BLUR) ?: false
                val menuBlurEnabled = prefs?.get(KEY_MENU_BG_BLUR) ?: prefs?.get(KEY_BLUR) ?: true
                val blurRadiusDp = prefs?.get(KEY_BLUR_RADIUS) ?: 4
                val blurEnabled = appLaunchBlurEnabled || edgeGradientBlurEnabled || menuBlurEnabled

                val lowResIcons = prefs?.get(KEY_LOW_RES) ?: false
                val autoIconSize = prefs?.get(KEY_ICON_SIZE_AUTO) ?: true
                val iconScalePreset = IconScalePreset.fromStorage(prefs?.get(KEY_ICON_SIZE_PRESET)).storageValue
                val listIconSize = prefs?.get(KEY_LIST_ICON_SIZE) ?: 48

                val animationOverrideEnabled = prefs?.get(KEY_ANIMATION_OVERRIDE) ?: true
                val splashIcon = prefs?.get(KEY_SPLASH_ICON) ?: true
                val splashDelay = prefs?.get(KEY_SPLASH_DELAY) ?: 500
                val appReturnAnimationDuration = prefs?.get(KEY_APP_RETURN_ANIM_DURATION) ?: 220

                val honeycombCols = prefs?.get(KEY_HONEYCOMB_COLS) ?: 4
                val honeycombFisheyeEnabled = prefs?.get(KEY_HONEYCOMB_FISHEYE) ?: true
                val honeycombTopBlur = prefs?.get(KEY_HONEYCOMB_TOP_BLUR) ?: 4
                val honeycombBottomBlur = prefs?.get(KEY_HONEYCOMB_BOTTOM_BLUR) ?: 4
                val honeycombTopFade = prefs?.get(KEY_HONEYCOMB_TOP_FADE) ?: 56
                val honeycombBottomFade = prefs?.get(KEY_HONEYCOMB_BOTTOM_FADE) ?: 56
                val showNotification = prefs?.get(KEY_SHOW_NOTIFICATION) ?: true

                LauncherSettingsSheet(
                    currentLayout = currentLayout,
                    blurEnabled = blurEnabled,
                    edgeBlurEnabled = edgeGradientBlurEnabled,
                    lowResIcons = lowResIcons,
                    animationOverrideEnabled = animationOverrideEnabled,
                    splashIcon = splashIcon,
                    splashDelay = splashDelay,
                    listIconSize = listIconSize,
                    honeycombCols = honeycombCols,
                    honeycombFisheyeEnabled = honeycombFisheyeEnabled,
                    honeycombTopBlur = honeycombTopBlur,
                    honeycombBottomBlur = honeycombBottomBlur,
                    honeycombTopFade = honeycombTopFade,
                    honeycombBottomFade = honeycombBottomFade,
                    showNotification = showNotification,
                    appLaunchBlurEnabled = appLaunchBlurEnabled,
                    edgeGradientBlurEnabled = edgeGradientBlurEnabled,
                    menuBlurEnabled = menuBlurEnabled,
                    blurRadiusDp = blurRadiusDp,
                    appReturnAnimationDuration = appReturnAnimationDuration,
                    iconScalePreset = iconScalePreset,
                    autoIconSize = autoIconSize,
                    onLayoutChange = { value -> scope.launch { dataStore.edit { it[KEY_LAYOUT] = value.name } } },
                    onBlurToggle = { enabled ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_BLUR] = enabled
                                it[KEY_APP_TRANSITION_BLUR] = enabled
                                it[KEY_EDGE_GRADIENT_BLUR] = enabled
                                it[KEY_EDGE_BLUR] = enabled
                                it[KEY_MENU_BG_BLUR] = enabled
                            }
                        }
                    },
                    onEdgeBlurToggle = { enabled ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_EDGE_GRADIENT_BLUR] = enabled
                                it[KEY_EDGE_BLUR] = enabled
                                it[KEY_BLUR] = enabled || appLaunchBlurEnabled || menuBlurEnabled
                            }
                        }
                    },
                    onLowResToggle = { enabled -> scope.launch { dataStore.edit { it[KEY_LOW_RES] = enabled } } },
                    onAnimationOverrideToggle = { enabled -> scope.launch { dataStore.edit { it[KEY_ANIMATION_OVERRIDE] = enabled } } },
                    onSplashToggle = { enabled -> scope.launch { dataStore.edit { it[KEY_SPLASH_ICON] = enabled } } },
                    onSplashDelayChange = { value -> scope.launch { dataStore.edit { it[KEY_SPLASH_DELAY] = value } } },
                    onListIconSizeChange = { value ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_ICON_SIZE_AUTO] = false
                                it[KEY_ICON_SIZE_PRESET] = IconScalePreset.CUSTOM.storageValue
                                it[KEY_LIST_ICON_SIZE] = value
                            }
                        }
                    },
                    onHoneycombColsChange = { value -> scope.launch { dataStore.edit { it[KEY_HONEYCOMB_COLS] = value } } },
                    onHoneycombFisheyeToggle = { value -> scope.launch { dataStore.edit { it[KEY_HONEYCOMB_FISHEYE] = value } } },
                    onHoneycombTopBlurChange = { value -> scope.launch { dataStore.edit { it[KEY_HONEYCOMB_TOP_BLUR] = value } } },
                    onHoneycombBottomBlurChange = { value -> scope.launch { dataStore.edit { it[KEY_HONEYCOMB_BOTTOM_BLUR] = value } } },
                    onHoneycombTopFadeChange = { value -> scope.launch { dataStore.edit { it[KEY_HONEYCOMB_TOP_FADE] = value } } },
                    onHoneycombBottomFadeChange = { value -> scope.launch { dataStore.edit { it[KEY_HONEYCOMB_BOTTOM_FADE] = value } } },
                    onShowNotificationChange = { value -> scope.launch { dataStore.edit { it[KEY_SHOW_NOTIFICATION] = value } } },
                    onAppLaunchBlurToggle = { enabled ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_APP_TRANSITION_BLUR] = enabled
                                it[KEY_BLUR] = enabled || edgeGradientBlurEnabled || menuBlurEnabled
                            }
                        }
                    },
                    onEdgeGradientBlurToggle = { enabled ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_EDGE_GRADIENT_BLUR] = enabled
                                it[KEY_EDGE_BLUR] = enabled
                                it[KEY_BLUR] = enabled || appLaunchBlurEnabled || menuBlurEnabled
                            }
                        }
                    },
                    onMenuBlurToggle = { enabled ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_MENU_BG_BLUR] = enabled
                                it[KEY_BLUR] = enabled || appLaunchBlurEnabled || edgeGradientBlurEnabled
                            }
                        }
                    },
                    onBlurRadiusChange = { value ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_BLUR_RADIUS] = value
                                it[KEY_HONEYCOMB_TOP_BLUR] = value
                                it[KEY_HONEYCOMB_BOTTOM_BLUR] = value
                            }
                        }
                    },
                    onAppReturnAnimationDurationChange = { value -> scope.launch { dataStore.edit { it[KEY_APP_RETURN_ANIM_DURATION] = value } } },
                    onIconScalePresetChange = { value ->
                        scope.launch {
                            dataStore.edit {
                                val preset = IconScalePreset.fromStorage(value)
                                it[KEY_ICON_SIZE_AUTO] = preset == IconScalePreset.AUTO
                                it[KEY_ICON_SIZE_PRESET] = preset.storageValue
                                if (preset != IconScalePreset.AUTO && preset != IconScalePreset.CUSTOM) {
                                    it[KEY_LIST_ICON_SIZE] = preset.listIconSizeDp
                                }
                            }
                        }
                    },
                    onAutoIconSizeToggle = { enabled ->
                        scope.launch {
                            dataStore.edit {
                                it[KEY_ICON_SIZE_AUTO] = enabled
                                if (enabled) {
                                    it[KEY_ICON_SIZE_PRESET] = IconScalePreset.AUTO.storageValue
                                }
                            }
                        }
                    },
                    onResetDefaults = {
                        scope.launch {
                            dataStore.edit {
                                it[KEY_LAYOUT] = LayoutMode.Honeycomb.name
                                it[KEY_BLUR] = true
                                it[KEY_APP_TRANSITION_BLUR] = true
                                it[KEY_EDGE_GRADIENT_BLUR] = false
                                it[KEY_EDGE_BLUR] = false
                                it[KEY_MENU_BG_BLUR] = true
                                it[KEY_BLUR_RADIUS] = 4
                                it[KEY_LOW_RES] = false
                                it[KEY_ICON_SIZE_AUTO] = true
                                it[KEY_ICON_SIZE_PRESET] = IconScalePreset.AUTO.storageValue
                                it[KEY_LIST_ICON_SIZE] = 48
                                it[KEY_ANIMATION_OVERRIDE] = true
                                it[KEY_SPLASH_ICON] = true
                                it[KEY_SPLASH_DELAY] = 500
                                it[KEY_APP_RETURN_ANIM_DURATION] = 220
                                it[KEY_HONEYCOMB_COLS] = 4
                                it[KEY_HONEYCOMB_FISHEYE] = true
                                it[KEY_HONEYCOMB_TOP_BLUR] = 4
                                it[KEY_HONEYCOMB_BOTTOM_BLUR] = 4
                                it[KEY_HONEYCOMB_TOP_FADE] = 56
                                it[KEY_HONEYCOMB_BOTTOM_FADE] = 56
                                it[KEY_SHOW_NOTIFICATION] = true
                            }
                        }
                    },
                    onDismiss = { finish() },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}
