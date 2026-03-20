package com.example.wlauncher.viewmodel

import android.app.Application
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.data.repository.AppRepository
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.navigation.ScreenState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)

    val apps: StateFlow<List<AppInfo>> = appRepository.apps

    private val _screenState = MutableStateFlow(ScreenState.Face)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.Honeycomb)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    // 模糊动画开关：API 31+ 默认开启，低于 31 默认关闭
    private val _blurEnabled = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    // 打开应用时记录图标在屏幕中的位置，用于 transformOrigin 动画
    private val _appOpenOrigin = MutableStateFlow(Offset(0.5f, 0.5f))
    val appOpenOrigin: StateFlow<Offset> = _appOpenOrigin.asStateFlow()

    // 当前打开的应用名
    private val _currentApp = MutableStateFlow<AppInfo?>(null)
    val currentApp: StateFlow<AppInfo?> = _currentApp.asStateFlow()

    private var launchJob: Job? = null
    private var returnToFaceJob: Job? = null
    private var launchedExternalApp = false

    fun setState(state: ScreenState) {
        if (state != ScreenState.App) {
            launchJob?.cancel()
            launchedExternalApp = false
            if (state == ScreenState.Face || state == ScreenState.Apps) {
                _currentApp.value = null
            }
        }
        _screenState.value = state
    }

    fun openApp(appInfo: AppInfo, origin: Offset = Offset(0.5f, 0.5f)) {
        launchJob?.cancel()
        returnToFaceJob?.cancel()
        _currentApp.value = appInfo
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App
        launchedExternalApp = false
        launchJob = viewModelScope.launch {
            delay(APP_LAUNCH_DELAY_MS)
            launchedExternalApp = true
            appRepository.launchApp(appInfo)
        }
    }

    fun closeApp() {
        launchJob?.cancel()
        launchedExternalApp = false
        _currentApp.value = null
        _screenState.value = ScreenState.Apps
    }

    fun goHome() {
        launchJob?.cancel()
        launchedExternalApp = false
        returnToFaceJob?.cancel()
        _currentApp.value = null
        _screenState.value = ScreenState.Face
    }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
    }

    fun setBlurEnabled(enabled: Boolean) {
        // 只有 API 31+ 才能真正启用模糊
        _blurEnabled.value = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    fun openSettings() {
        _screenState.value = ScreenState.Settings
    }

    fun onHomePressed() {
        when (_screenState.value) {
            ScreenState.Face -> _screenState.value = ScreenState.Apps
            ScreenState.Apps,
            ScreenState.Settings,
            ScreenState.Stack,
            ScreenState.Notifications,
            ScreenState.ControlCenter -> goHome()
            ScreenState.App -> {
                if (launchJob?.isActive == true) {
                    closeApp()
                } else {
                    goHome()
                }
            }
        }
    }

    fun onBackPressed() {
        when (_screenState.value) {
            ScreenState.Settings -> _screenState.value = ScreenState.Apps
            ScreenState.Apps,
            ScreenState.Stack,
            ScreenState.Notifications,
            ScreenState.ControlCenter -> goHome()
            ScreenState.App -> closeApp()
            ScreenState.Face -> Unit
        }
    }

    fun onLauncherResumed() {
        if (!launchedExternalApp) return

        launchedExternalApp = false
        launchJob = null
        _currentApp.value = null
        _screenState.value = ScreenState.Apps

        returnToFaceJob?.cancel()
        returnToFaceJob = viewModelScope.launch {
            delay(APP_RETURN_ANIMATION_MS)
            if (_screenState.value == ScreenState.Apps) {
                _screenState.value = ScreenState.Face
            }
        }
    }

    override fun onCleared() {
        launchJob?.cancel()
        returnToFaceJob?.cancel()
        super.onCleared()
        appRepository.destroy()
    }

    private companion object {
        const val APP_LAUNCH_DELAY_MS = 420L
        const val APP_RETURN_ANIMATION_MS = 320L
    }
}
