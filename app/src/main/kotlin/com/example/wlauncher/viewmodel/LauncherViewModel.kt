package com.example.wlauncher.viewmodel

import android.app.Application
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.data.repository.AppRepository
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.navigation.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun setState(state: ScreenState) {
        _screenState.value = state
    }

    fun openApp(appInfo: AppInfo, origin: Offset = Offset(0.5f, 0.5f)) {
        _currentApp.value = appInfo
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App
        appRepository.launchApp(appInfo)
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
        // 只有 API 31+ 才能真正启用模糊
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
