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

    private val _blurEnabled = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _appOpenOrigin = MutableStateFlow(Offset(0.5f, 0.5f))
    val appOpenOrigin: StateFlow<Offset> = _appOpenOrigin.asStateFlow()

    private val _currentApp = MutableStateFlow<AppInfo?>(null)
    val currentApp: StateFlow<AppInfo?> = _currentApp.asStateFlow()

    // 是否正在启动外部应用（用于区分 onResume 来源）
    private var launchingExternalApp = false
    private var launchJob: Job? = null

    fun setState(state: ScreenState) {
        _screenState.value = state
    }

    /**
     * 打开应用：先播放退出动画，等动画完成后再启动外部 Activity。
     */
    fun openApp(appInfo: AppInfo, origin: Offset = Offset(0.5f, 0.5f)) {
        _currentApp.value = appInfo
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App

        launchJob?.cancel()
        launchJob = viewModelScope.launch {
            // 等待退出动画播放完 (~500ms)
            delay(500)
            launchingExternalApp = true
            appRepository.launchApp(appInfo)
        }
    }

    /**
     * 从外部应用返回桌面时调用。
     * 播放返回动画：从 App 状态回到 Apps 状态。
     */
    fun onReturnToLauncher() {
        if (launchingExternalApp) {
            launchingExternalApp = false
            // 回到应用列表，触发返回动画
            _screenState.value = ScreenState.Apps
        }
    }

    /**
     * 主页键：表盘 ↔ 应用列表 切换，其他状态一律回表盘。
     */
    fun handleHomePress() {
        when (_screenState.value) {
            ScreenState.Face -> _screenState.value = ScreenState.Apps
            ScreenState.Apps -> _screenState.value = ScreenState.Face
            else -> _screenState.value = ScreenState.Face
        }
    }

    /**
     * 返回键：按层级回退。表盘不响应。
     */
    fun handleBackPress() {
        when (_screenState.value) {
            ScreenState.Face -> { /* 表盘不响应返回键 */ }
            ScreenState.Apps -> _screenState.value = ScreenState.Face
            ScreenState.App -> {
                // 动画未完成，取消启动
                launchJob?.cancel()
                launchJob = null
                launchingExternalApp = false
                _screenState.value = ScreenState.Apps
            }
            ScreenState.Settings -> _screenState.value = ScreenState.Apps
            ScreenState.Stack -> _screenState.value = ScreenState.Face
            ScreenState.Notifications -> _screenState.value = ScreenState.Face
            ScreenState.ControlCenter -> _screenState.value = ScreenState.Face
        }
    }

    fun closeApp() {
        _screenState.value = ScreenState.Apps
    }

    fun goHome() {
        _screenState.value = ScreenState.Face
    }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
    }

    fun setBlurEnabled(enabled: Boolean) {
        _blurEnabled.value = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    fun openSettings() {
        _screenState.value = ScreenState.Settings
    }

    override fun onCleared() {
        super.onCleared()
        appRepository.destroy()
    }
}
