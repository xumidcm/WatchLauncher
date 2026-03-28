package com.example.wlauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wlauncher.config.IconScalePreset
import com.example.wlauncher.config.resolveScaleMultiplier
import com.example.wlauncher.ui.anim.appListLayerValues
import com.example.wlauncher.ui.anim.faceLayerValues
import com.example.wlauncher.ui.anim.notificationLayerValues
import com.example.wlauncher.ui.anim.scaleBlurAlpha
import com.example.wlauncher.ui.anim.stackLayerValues
import com.example.wlauncher.ui.drawer.HoneycombScreen
import com.example.wlauncher.ui.drawer.ListDrawerScreen
import com.example.wlauncher.ui.home.WatchFaceLayer
import com.example.wlauncher.ui.navigation.GestureHost
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.navigation.ScreenState
import com.example.wlauncher.ui.notification.NotificationLayer
import com.example.wlauncher.ui.settings.LauncherSettingsSheet
import com.example.wlauncher.ui.smartstack.SmartStackLayer
import com.example.wlauncher.ui.theme.WatchLauncherTheme
import com.example.wlauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay

private const val BASE_LAUNCH_MASK_DELAY_MS = 180L

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
        if (::vm.isInitialized && vm.animationOverrideEnabled.value) {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.launcher_return_cupertino_enter, R.anim.launcher_return_cupertino_exit)
        }
        if (::vm.isInitialized) vm.onReturnToLauncher()
    }

    override fun onPause() {
        super.onPause()
        if (::vm.isInitialized && vm.animationOverrideEnabled.value) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val screenState by vm.screenState.collectAsState()
    val layoutMode by vm.layoutMode.collectAsState()
    val blurEnabled by vm.blurEnabled.collectAsState()
    val appLaunchBlurEnabled by vm.appLaunchBlurEnabled.collectAsState()
    val edgeBlurEnabled by vm.edgeBlurEnabled.collectAsState()
    val menuBlurEnabled by vm.menuBackgroundBlurEnabled.collectAsState()
    val animationOverrideEnabled by vm.animationOverrideEnabled.collectAsState()
    val apps by vm.apps.collectAsState()
    val appOpenOrigin by vm.appOpenOrigin.collectAsState()
    val splashIcon by vm.splashIcon.collectAsState()
    val splashDelay by vm.splashDelay.collectAsState()
    val appReturnAnimationDuration by vm.appReturnAnimationDuration.collectAsState()
    val currentApp by vm.currentApp.collectAsState()
    val listIconSize by vm.listIconSize.collectAsState()
    val iconScalePreset by vm.iconSizePreset.collectAsState()
    val autoIconSize by vm.iconSizeAuto.collectAsState()
    val blurRadiusDp by vm.blurRadiusDp.collectAsState()
    val listScrollIndex by vm.listScrollIndex.collectAsState()
    val listScrollOffset by vm.listScrollOffset.collectAsState()
    val honeycombScrollOffset by vm.honeycombScrollOffset.collectAsState()
    val honeycombCols by vm.honeycombCols.collectAsState()
    val honeycombTopFade by vm.honeycombTopFade.collectAsState()
    val honeycombBottomFade by vm.honeycombBottomFade.collectAsState()
    val showNotification by vm.showNotification.collectAsState()

    val layerBlurEnabled = when (screenState) {
        ScreenState.App -> appLaunchBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        else -> blurEnabled
    }
    val reduceLegacyDrawerEffects = Build.VERSION.SDK_INT < Build.VERSION_CODES.S && screenState == ScreenState.App

    var prevState by remember { mutableStateOf(screenState) }
    val isReturningFromApp = prevState == ScreenState.App && screenState == ScreenState.Apps
    LaunchedEffect(screenState) { prevState = screenState }

    val useOrigin = screenState == ScreenState.App || isReturningFromApp

    val showLaunchBackdrop = screenState == ScreenState.App && currentApp != null
    var showSplash by remember { mutableStateOf(false) }
    LaunchedEffect(screenState, splashIcon, splashDelay, currentApp) {
        if (screenState == ScreenState.App && splashIcon && currentApp != null) {
            showSplash = false
            delay(BASE_LAUNCH_MASK_DELAY_MS)
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
        val configuration = LocalConfiguration.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val iconPreset = IconScalePreset.fromStorage(iconScalePreset)
        val iconScaleMultiplier = iconPreset.resolveScaleMultiplier(configuration.smallestScreenWidthDp)

        GestureHost(
            screenState = screenState,
            onStateChange = { vm.setState(it) },
            showNotification = showNotification,
            showControlCenter = false,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = faceLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = layerBlurEnabled
                    )
            ) { WatchFaceLayer() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = appListLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = layerBlurEnabled,
                        origin = if (useOrigin) appOpenOrigin else null
                    )
            ) {
                when (layoutMode) {
                    LayoutMode.Honeycomb -> HoneycombScreen(
                        apps = apps,
                        blurEnabled = appLaunchBlurEnabled,
                        edgeBlurEnabled = edgeBlurEnabled,
                        suppressHeavyEffects = reduceLegacyDrawerEffects,
                        narrowCols = honeycombCols,
                        iconScaleMultiplier = iconScaleMultiplier,
                        topFadeRangeDp = honeycombTopFade,
                        bottomFadeRangeDp = honeycombBottomFade,
                        blurRadiusDp = blurRadiusDp,
                        initialScrollOffset = honeycombScrollOffset,
                        onScrollOffsetChange = { vm.setHoneycombDrawerScrollOffset(it) },
                        onAppClick = { appInfo, origin ->
                            val launchDelay = BASE_LAUNCH_MASK_DELAY_MS + if (splashIcon) splashDelay.toLong() else 0L
                            vm.openApp(appInfo, origin, launchDelay)
                        },
                        onReorder = { from, to -> vm.swapApps(from, to) },
                        onLongClick = {},
                        menuBlurEnabled = menuBlurEnabled,
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )

                    LayoutMode.List -> ListDrawerScreen(
                        apps = apps,
                        blurEnabled = appLaunchBlurEnabled,
                        edgeBlurEnabled = edgeBlurEnabled,
                        suppressHeavyEffects = reduceLegacyDrawerEffects,
                        iconSize = listIconSize.dp,
                        iconScaleMultiplier = iconScaleMultiplier,
                        menuBlurEnabled = menuBlurEnabled,
                        blurRadiusDp = blurRadiusDp,
                        initialFirstVisibleItemIndex = listScrollIndex,
                        initialFirstVisibleItemOffset = listScrollOffset,
                        onScrollPositionChange = { index, offset -> vm.setListDrawerScrollPosition(index, offset) },
                        onAppClick = { appInfo, origin ->
                            val launchDelay = BASE_LAUNCH_MASK_DELAY_MS + if (splashIcon) splashDelay.toLong() else 0L
                            vm.openApp(appInfo, origin, launchDelay)
                        },
                        onReorder = { from, to -> vm.swapApps(from, to) },
                        onLongClick = {},
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
                visible = showLaunchBackdrop,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(durationMillis = appReturnAnimationDuration))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVisibility(
                        visible = showSplash && splashIcon && currentApp != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.5f),
                        exit = fadeOut(animationSpec = tween(durationMillis = appReturnAnimationDuration)) +
                            scaleOut(targetScale = 0.3f, animationSpec = tween(durationMillis = appReturnAnimationDuration))
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
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scaleBlurAlpha(
                        targetValues = stackLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = layerBlurEnabled
                    )
            ) { SmartStackLayer() }

            if (showNotification) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scaleBlurAlpha(
                            targetValues = notificationLayerValues(screenState),
                            screenHeight = screenHeightPx,
                            blurEnabled = layerBlurEnabled
                        )
                ) { NotificationLayer() }
            }

            if (screenState == ScreenState.Settings) {
                LauncherSettingsSheet(
                    currentLayout = layoutMode,
                    blurEnabled = blurEnabled,
                    edgeBlurEnabled = edgeBlurEnabled,
                    lowResIcons = vm.lowResIcons.collectAsState().value,
                    animationOverrideEnabled = animationOverrideEnabled,
                    splashIcon = splashIcon,
                    splashDelay = splashDelay,
                    listIconSize = listIconSize,
                    honeycombCols = honeycombCols,
                    honeycombTopFade = honeycombTopFade,
                    honeycombBottomFade = honeycombBottomFade,
                    showNotification = showNotification,
                    appLaunchBlurEnabled = appLaunchBlurEnabled,
                    edgeGradientBlurEnabled = edgeBlurEnabled,
                    menuBlurEnabled = menuBlurEnabled,
                    blurRadiusDp = blurRadiusDp,
                    appReturnAnimationDuration = appReturnAnimationDuration,
                    iconScalePreset = iconPreset.storageValue,
                    autoIconSize = autoIconSize,
                    onLayoutChange = { vm.setLayoutMode(it) },
                    onBlurToggle = { vm.setBlurEnabled(it) },
                    onEdgeBlurToggle = { vm.setEdgeGradientBlurEnabled(it) },
                    onLowResToggle = { vm.setLowResIcons(it) },
                    onAnimationOverrideToggle = { vm.setAnimationOverrideEnabled(it) },
                    onSplashToggle = { vm.setSplashIcon(it) },
                    onSplashDelayChange = { vm.setSplashDelay(it) },
                    onListIconSizeChange = { vm.setListIconSize(it) },
                    onHoneycombColsChange = { vm.setHoneycombCols(it) },
                    onHoneycombTopFadeChange = { vm.setHoneycombTopFade(it) },
                    onHoneycombBottomFadeChange = { vm.setHoneycombBottomFade(it) },
                    onShowNotificationChange = { vm.setShowNotification(it) },
                    onAppLaunchBlurToggle = { vm.setAppLaunchBlurEnabled(it) },
                    onEdgeGradientBlurToggle = { vm.setEdgeGradientBlurEnabled(it) },
                    onMenuBlurToggle = { vm.setMenuBackgroundBlurEnabled(it) },
                    onBlurRadiusChange = { vm.setBlurRadiusDp(it) },
                    onAppReturnAnimationDurationChange = { vm.setAppReturnAnimationDuration(it) },
                    onIconScalePresetChange = { vm.setIconSizePreset(it) },
                    onAutoIconSizeToggle = { vm.setIconSizeAuto(it) },
                    onResetDefaults = { vm.resetSettings() },
                    onDismiss = { vm.setState(ScreenState.Apps) }
                )
            }
        }
    }
}
