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
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.settings.LauncherSettingsSheet
import com.example.wlauncher.ui.theme.WatchLauncherTheme
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_EDGE_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_BOTTOM_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_BOTTOM_FADE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_COLS
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_TOP_BLUR
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_HONEYCOMB_TOP_FADE
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_LAYOUT
import com.example.wlauncher.viewmodel.LauncherViewModel.Companion.KEY_LOW_RES
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

                val layoutMode = prefs?.get(KEY_LAYOUT)?.let {
                    try {
                        LayoutMode.valueOf(it)
                    } catch (_: Exception) {
                        LayoutMode.Honeycomb
                    }
                } ?: LayoutMode.Honeycomb
                val blurEnabled = prefs?.get(KEY_BLUR) ?: true
                val edgeBlurEnabled = prefs?.get(KEY_EDGE_BLUR) ?: false
                val lowRes = prefs?.get(KEY_LOW_RES) ?: false
                val splash = prefs?.get(KEY_SPLASH_ICON) ?: true
                val delay = prefs?.get(KEY_SPLASH_DELAY) ?: 500
                val honeycombCols = prefs?.get(KEY_HONEYCOMB_COLS) ?: 4
                val topBlur = prefs?.get(KEY_HONEYCOMB_TOP_BLUR) ?: 12
                val bottomBlur = prefs?.get(KEY_HONEYCOMB_BOTTOM_BLUR) ?: 12
                val topFade = prefs?.get(KEY_HONEYCOMB_TOP_FADE) ?: 56
                val bottomFade = prefs?.get(KEY_HONEYCOMB_BOTTOM_FADE) ?: 56

                LauncherSettingsSheet(
                    currentLayout = layoutMode,
                    blurEnabled = blurEnabled,
                    edgeBlurEnabled = edgeBlurEnabled,
                    lowResIcons = lowRes,
                    splashIcon = splash,
                    splashDelay = delay,
                    honeycombCols = honeycombCols,
                    honeycombTopBlur = topBlur,
                    honeycombBottomBlur = bottomBlur,
                    honeycombTopFade = topFade,
                    honeycombBottomFade = bottomFade,
                    onLayoutChange = { scope.launch { dataStore.edit { p -> p[KEY_LAYOUT] = it.name } } },
                    onBlurToggle = {
                        scope.launch {
                            dataStore.edit { p ->
                                p[KEY_BLUR] = it
                                if (!it) {
                                    p[KEY_EDGE_BLUR] = false
                                }
                            }
                        }
                    },
                    onEdgeBlurToggle = { scope.launch { dataStore.edit { p -> p[KEY_EDGE_BLUR] = it && blurEnabled } } },
                    onLowResToggle = { scope.launch { dataStore.edit { p -> p[KEY_LOW_RES] = it } } },
                    onSplashToggle = { scope.launch { dataStore.edit { p -> p[KEY_SPLASH_ICON] = it } } },
                    onSplashDelayChange = { scope.launch { dataStore.edit { p -> p[KEY_SPLASH_DELAY] = it } } },
                    onHoneycombColsChange = { scope.launch { dataStore.edit { p -> p[KEY_HONEYCOMB_COLS] = it } } },
                    onHoneycombTopBlurChange = { scope.launch { dataStore.edit { p -> p[KEY_HONEYCOMB_TOP_BLUR] = it } } },
                    onHoneycombBottomBlurChange = { scope.launch { dataStore.edit { p -> p[KEY_HONEYCOMB_BOTTOM_BLUR] = it } } },
                    onHoneycombTopFadeChange = { scope.launch { dataStore.edit { p -> p[KEY_HONEYCOMB_TOP_FADE] = it } } },
                    onHoneycombBottomFadeChange = { scope.launch { dataStore.edit { p -> p[KEY_HONEYCOMB_BOTTOM_FADE] = it } } },
                    onDismiss = { finish() },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}
