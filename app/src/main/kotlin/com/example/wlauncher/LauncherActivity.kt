package com.example.wlauncher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.wlauncher.ui.anim.*
import com.example.wlauncher.ui.theme.WatchLauncherTheme
import com.example.wlauncher.viewmodel.LauncherViewModel
import androidx.compose.ui.input.pointer.pointerInput
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
    val apps by vm.apps.collectAsState()
    val appOpenOrigin by vm.appOpenOrigin.collectAsState()
    val splashIcon by vm.splashIcon.collectAsState()
    val splashDelay by vm.splashDelay.collectAsState()
    val currentApp by vm.currentApp.collectAsState()
    val listIconSize by vm.listIconSize.collectAsState()
    val honeycombCols by vm.honeycombCols.collectAsState()
    val showSteps by vm.showSteps.collectAsState()
    val stepGoal by vm.stepGoal.collectAsState()
    val showNotification by vm.showNotification.collectAsState()
    val showControlCenter by vm.showControlCenter.collectAsState()
    val firstRun by vm.firstRun.collectAsState()

    // 跟踪上一个状态，判断是否从 App 返回
    var prevState by remember { mutableStateOf(screenState) }
    val isReturningFromApp = prevState == ScreenState.App && screenState == ScreenState.Apps
    LaunchedEffect(screenState) { prevState = screenState }

    // 只在打开/返回应用时使用图标原点，Face↔Apps 固定中心缩放
    val useOrigin = screenState == ScreenState.App || isReturningFromApp

    // 启动遮罩延迟显示
    var showSplash by remember { mutableStateOf(false) }
    LaunchedEffect(screenState) {
        if (screenState == ScreenState.App && splashIcon) {
            showSplash = false
            delay((splashDelay * 0.7f).toLong()) // 等 appListLayer 缩放飞出后
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
            // Layer 1: Watch Face
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = faceLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) { WatchFaceLayer(showSteps = showSteps, stepGoal = stepGoal) }

            // Layer 2: App Drawer
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
                        narrowCols = honeycombCols,
                        onAppClick = { appInfo, origin -> vm.openApp(appInfo, origin) },
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )
                    LayoutMode.List -> ListDrawerScreen(
                        apps = apps,
                        blurEnabled = blurEnabled,
                        iconSize = listIconSize.dp,
                        onAppClick = { appInfo, origin -> vm.openApp(appInfo, origin) },
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )
                }
            }

            // 动画过渡中阻止触摸（覆盖在 drawer 上层）
            if (screenState == ScreenState.App) {
                Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) { awaitPointerEvent().changes.forEach { it.consume() } }
                    }
                })
            }

            // 启动遮罩 — 黑场 + 居中应用图标
            AnimatedVisibility(
                visible = showSplash && screenState == ScreenState.App && currentApp != null,
                enter = fadeIn() + scaleIn(initialScale = 0.5f),
                exit = fadeOut() + scaleOut(targetScale = 0.3f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    currentApp?.let { app ->
                        Image(
                            bitmap = app.cachedIcon,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Layer 3: Smart Stack
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = stackLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = blurEnabled
                    )
            ) { SmartStackLayer() }

            // Layer 4: Notifications (conditional)
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

            // Layer 5: Control Center (conditional)
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

            // Layer 6: Settings
            if (screenState == ScreenState.Settings) {
                val iconPackState by vm.iconPack.collectAsState()
                var iconPackRefresh by remember { mutableIntStateOf(0) }
                val iconPacks = remember(iconPackRefresh) { vm.getIconPackManager().getInstalledIconPacks().map { it.packageName to it.label } }
                LauncherSettingsSheet(
                    currentLayout = layoutMode,
                    blurEnabled = blurEnabled,
                    lowResIcons = vm.lowResIcons.collectAsState().value,
                    splashIcon = splashIcon,
                    splashDelay = splashDelay,
                    listIconSize = listIconSize,
                    honeycombCols = honeycombCols,
                    iconPackName = iconPackState,
                    iconPacks = iconPacks,
                    showSteps = showSteps,
                    stepGoal = stepGoal,
                    showNotification = showNotification,
                    showControlCenter = showControlCenter,
                    onLayoutChange = { vm.setLayoutMode(it) },
                    onBlurToggle = { vm.setBlurEnabled(it) },
                    onLowResToggle = { vm.setLowResIcons(it) },
                    onSplashToggle = { vm.setSplashIcon(it) },
                    onSplashDelayChange = { vm.setSplashDelay(it) },
                    onListIconSizeChange = { vm.setListIconSize(it) },
                    onHoneycombColsChange = { vm.setHoneycombCols(it) },
                    onIconPackChange = { vm.setIconPack(it) },
                    onShowStepsChange = { vm.setShowSteps(it) },
                    onStepGoalChange = { vm.setStepGoal(it) },
                    onShowNotificationChange = { vm.setShowNotification(it) },
                    onShowControlCenterChange = { vm.setShowControlCenter(it) },
                    onDismiss = { vm.setState(ScreenState.Apps) }
                )
            }
        }

        // Layer 7: First Run Permission Request
        if (firstRun) {
            PermissionRequestSheet(
                onDismiss = { vm.setFirstRun(false) }
            )
        }
    }
}
