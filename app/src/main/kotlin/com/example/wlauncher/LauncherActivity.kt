package com.example.wlauncher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.example.wlauncher.ui.controlcenter.ControlCenterLayer
import com.example.wlauncher.ui.drawer.HoneycombScreen
import com.example.wlauncher.ui.drawer.ListDrawerScreen
import com.example.wlauncher.ui.home.WatchFaceLayer
import com.example.wlauncher.ui.navigation.GestureHost
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.navigation.ScreenState
import com.example.wlauncher.ui.notification.NotificationLayer
import com.example.wlauncher.ui.settings.LauncherSettingsSheet
import com.example.wlauncher.ui.smartstack.SmartStackLayer
import com.example.wlauncher.ui.anim.*
import com.example.wlauncher.ui.theme.WatchLauncherTheme
import com.example.wlauncher.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            WatchLauncherTheme {
                LauncherScreen(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.onHomePressed()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onLauncherResumed()
    }
}

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val screenState by vm.screenState.collectAsState()
    val layoutMode by vm.layoutMode.collectAsState()
    val blurEnabled by vm.blurEnabled.collectAsState()
    val apps by vm.apps.collectAsState()

    BackHandler {
        vm.onBackPressed()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }

        GestureHost(
            screenState = screenState,
            onStateChange = { vm.setState(it) },
            modifier = Modifier.fillMaxSize()
        ) {
            // Layer 1: Watch Face
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = faceLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) {
                WatchFaceLayer(
                    onTap = {}
                )
            }

            // Layer 2: App Drawer (Honeycomb or List)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = appListLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) {
                when (layoutMode) {
                    LayoutMode.Honeycomb -> HoneycombScreen(
                        apps = apps,
                        onAppClick = { appInfo, origin ->
                            vm.openApp(appInfo, origin)
                        },
                        onSettingsClick = { vm.openSettings() }
                    )
                    LayoutMode.List -> ListDrawerScreen(
                        apps = apps,
                        onAppClick = { appInfo ->
                            vm.openApp(appInfo)
                        },
                        onSettingsClick = { vm.openSettings() }
                    )
                }
            }

            // Layer 3: Smart Stack (swipe up from face)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = stackLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) {
                SmartStackLayer()
            }

            // Layer 4: Notification Center (swipe down from face)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = notificationLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) {
                NotificationLayer()
            }

            // Layer 5: Control Center (side button)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = controlCenterLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) {
                ControlCenterLayer()
            }

            // Layer 6: App Launch Transition
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .scaleBlurAlpha(
                        targetValues = appViewLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = false
                    )
            )

            // Layer 7: Settings
            if (screenState == ScreenState.Settings) {
                LauncherSettingsSheet(
                    currentLayout = layoutMode,
                    blurEnabled = blurEnabled,
                    onLayoutChange = { vm.setLayoutMode(it) },
                    onBlurToggle = { vm.setBlurEnabled(it) },
                    onDismiss = { vm.setState(ScreenState.Apps) }
                )
            }
        }
    }
}
