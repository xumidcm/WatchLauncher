package com.example.wlauncher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wlauncher.ui.anim.appListLayerValues
import com.example.wlauncher.ui.anim.controlCenterLayerValues
import com.example.wlauncher.ui.anim.faceLayerValues
import com.example.wlauncher.ui.anim.notificationLayerValues
import com.example.wlauncher.ui.anim.scaleBlurAlpha
import com.example.wlauncher.ui.anim.stackLayerValues
import com.example.wlauncher.ui.controlcenter.ControlCenterLayer
import com.example.wlauncher.ui.drawer.HoneycombScreen
import com.example.wlauncher.ui.drawer.ListDrawerScreen
import com.example.wlauncher.ui.home.WatchFaceLayer
import com.example.wlauncher.ui.navigation.GestureHost
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.navigation.ScreenState
import com.example.wlauncher.ui.notification.NotificationLayer
import com.example.wlauncher.ui.onboarding.PermissionRequestSheet
import com.example.wlauncher.ui.settings.LauncherSettingsSheet
import com.example.wlauncher.ui.smartstack.SmartStackLayer
import com.example.wlauncher.ui.theme.WatchLauncherTheme
import com.example.wlauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay

class LauncherActivity : ComponentActivity() {

    private lateinit var vm: LauncherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler(applicationContext).install()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        onBackPressedDispatcher.addCallback(this) { vm.handleBackPress() }
        setContent {
            WatchLauncherTheme {
                val viewModel: LauncherViewModel = viewModel()
                vm = viewModel
                LauncherScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::vm.isInitialized) vm.handleHomePress()
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        if (::vm.isInitialized) vm.onReturnToLauncher()
    }

    override fun onPause() {
        super.onPause()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val screenState by vm.screenState.collectAsState()
    val layoutMode by vm.layoutMode.collectAsState()
    val blurEnabled by vm.blurEnabled.collectAsState()
    val edgeBlurEnabled by vm.edgeBlurEnabled.collectAsState()
    val apps by vm.apps.collectAsState()
    val appOpenOrigin by vm.appOpenOrigin.collectAsState()
    val splashIcon by vm.splashIcon.collectAsState()
    val splashDelay by vm.splashDelay.collectAsState()
    val currentApp by vm.currentApp.collectAsState()
    val listIconSize by vm.listIconSize.collectAsState()
    val listTextSize by vm.listTextSize.collectAsState()
    val honeycombCols by vm.honeycombCols.collectAsState()
    val honeycombTopBlur by vm.honeycombTopBlur.collectAsState()
    val honeycombBottomBlur by vm.honeycombBottomBlur.collectAsState()
    val honeycombTopFade by vm.honeycombTopFade.collectAsState()
    val honeycombBottomFade by vm.honeycombBottomFade.collectAsState()
    val showSteps by vm.showSteps.collectAsState()
    val stepGoal by vm.stepGoal.collectAsState()
    val showNotification by vm.showNotification.collectAsState()
    val showControlCenter by vm.showControlCenter.collectAsState()
    val firstRun by vm.firstRun.collectAsState()

    var prevState by remember { mutableStateOf(screenState) }
    val isReturningFromApp = prevState == ScreenState.App && screenState == ScreenState.Apps
    LaunchedEffect(screenState) { prevState = screenState }

    val useOrigin = screenState == ScreenState.App || isReturningFromApp

    var showSplash by remember { mutableStateOf(false) }
    LaunchedEffect(screenState, splashIcon, splashDelay) {
        if (screenState == ScreenState.App && splashIcon) {
            showSplash = false
            delay((splashDelay * 0.7f).toLong())
            showSplash = true
        } else {
            showSplash = false
        }
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
            showNotification = showNotification,
            showControlCenter = showControlCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = faceLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) { WatchFaceLayer(showSteps = showSteps, stepGoal = stepGoal) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = appListLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled,
                        origin = if (useOrigin) appOpenOrigin else null
                    )
            ) {
                when (layoutMode) {
                    LayoutMode.Honeycomb -> HoneycombScreen(
                        apps = apps,
                        blurEnabled = blurEnabled,
                        edgeBlurEnabled = edgeBlurEnabled,
                        narrowCols = honeycombCols,
                        topBlurRadiusDp = honeycombTopBlur,
                        bottomBlurRadiusDp = honeycombBottomBlur,
                        topFadeRangeDp = honeycombTopFade,
                        bottomFadeRangeDp = honeycombBottomFade,
                        onAppClick = { appInfo, origin -> vm.openApp(appInfo, origin) },
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )
                    LayoutMode.List -> ListDrawerScreen(
                        apps = apps,
                        blurEnabled = blurEnabled,
                        edgeBlurEnabled = edgeBlurEnabled,
                        iconSize = listIconSize.dp,
                        textSizeSp = listTextSize,
                        onAppClick = { appInfo, origin -> vm.openApp(appInfo, origin) },
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )
                }
            }

            if (screenState == ScreenState.App) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent().changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }

            AnimatedVisibility(
                visible = showSplash && screenState == ScreenState.App && currentApp != null,
                enter = fadeIn() + scaleIn(initialScale = 0.5f),
                exit = fadeOut() + scaleOut(targetScale = 0.3f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    currentApp?.let { app ->
                        Image(
                            bitmap = app.cachedIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = stackLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) { SmartStackLayer() }

            if (showNotification) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaleBlurAlpha(
                            targetValues = notificationLayerValues(screenState),
                            screenHeight = screenHeightPx,
                            blurEnabled = blurEnabled
                        )
                ) { NotificationLayer() }
            }

            if (showControlCenter) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaleBlurAlpha(
                            targetValues = controlCenterLayerValues(screenState),
                            screenHeight = screenHeightPx,
                            blurEnabled = blurEnabled
                        )
                ) { ControlCenterLayer() }
            }

            if (screenState == ScreenState.Settings) {
                LauncherSettingsSheet(
                    currentLayout = layoutMode,
                    blurEnabled = blurEnabled,
                    edgeBlurEnabled = edgeBlurEnabled,
                    lowResIcons = vm.lowResIcons.collectAsState().value,
                    splashIcon = splashIcon,
                    splashDelay = splashDelay,
                    listIconSize = listIconSize,
                    listTextSize = listTextSize,
                    honeycombCols = honeycombCols,
                    honeycombTopBlur = honeycombTopBlur,
                    honeycombBottomBlur = honeycombBottomBlur,
                    honeycombTopFade = honeycombTopFade,
                    honeycombBottomFade = honeycombBottomFade,
                    showSteps = showSteps,
                    stepGoal = stepGoal,
                    showNotification = showNotification,
                    showControlCenter = showControlCenter,
                    onLayoutChange = { vm.setLayoutMode(it) },
                    onBlurToggle = { vm.setBlurEnabled(it) },
                    onEdgeBlurToggle = { vm.setEdgeBlurEnabled(it) },
                    onLowResToggle = { vm.setLowResIcons(it) },
                    onSplashToggle = { vm.setSplashIcon(it) },
                    onSplashDelayChange = { vm.setSplashDelay(it) },
                    onListIconSizeChange = { vm.setListIconSize(it) },
                    onListTextSizeChange = { vm.setListTextSize(it) },
                    onHoneycombColsChange = { vm.setHoneycombCols(it) },
                    onHoneycombTopBlurChange = { vm.setHoneycombTopBlur(it) },
                    onHoneycombBottomBlurChange = { vm.setHoneycombBottomBlur(it) },
                    onHoneycombTopFadeChange = { vm.setHoneycombTopFade(it) },
                    onHoneycombBottomFadeChange = { vm.setHoneycombBottomFade(it) },
                    onShowStepsChange = { vm.setShowSteps(it) },
                    onStepGoalChange = { vm.setStepGoal(it) },
                    onShowNotificationChange = { vm.setShowNotification(it) },
                    onShowControlCenterChange = { vm.setShowControlCenter(it) },
                    onResetDefaults = { vm.resetSettings() },
                    onDismiss = { vm.setState(ScreenState.Apps) }
                )
            }
        }

        if (firstRun) {
            PermissionRequestSheet(
                onDismiss = { vm.setFirstRun(false) }
            )
        }
    }
}
