package com.example.wlauncher.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.data.repository.AppRepository
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.navigation.ScreenState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val KEY_LAYOUT = stringPreferencesKey("layout_mode")
        val KEY_BLUR = booleanPreferencesKey("blur_enabled")
        val KEY_LOW_RES = booleanPreferencesKey("low_res_icons")
        val KEY_SPLASH_ICON = booleanPreferencesKey("splash_icon")
        val KEY_SPLASH_DELAY = intPreferencesKey("splash_delay")
        val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
        val KEY_APP_ORDER = stringPreferencesKey("app_order")
    }

    private val store = application.dataStore
    private val appRepository = AppRepository(application)

    val apps: StateFlow<List<AppInfo>> = appRepository.apps

    private val _screenState = MutableStateFlow(ScreenState.Face)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.Honeycomb)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _blurEnabled = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _lowResIcons = MutableStateFlow(false)
    val lowResIcons: StateFlow<Boolean> = _lowResIcons.asStateFlow()

    private val _splashDelay = MutableStateFlow(500)
    val splashDelay: StateFlow<Int> = _splashDelay.asStateFlow()

    private val _iconPack = MutableStateFlow<String?>(null)
    val iconPack: StateFlow<String?> = _iconPack.asStateFlow()

    private val _appOrder = MutableStateFlow<List<String>>(emptyList())
    val appOrder: StateFlow<List<String>> = _appOrder.asStateFlow()

    private val _splashIcon = MutableStateFlow(true)
    val splashIcon: StateFlow<Boolean> = _splashIcon.asStateFlow()

    private val _appOpenOrigin = MutableStateFlow(Offset(0.5f, 0.5f))
    val appOpenOrigin: StateFlow<Offset> = _appOpenOrigin.asStateFlow()

    private val _currentApp = MutableStateFlow<AppInfo?>(null)
    val currentApp: StateFlow<AppInfo?> = _currentApp.asStateFlow()

    private var launchingExternalApp = false
    private var launchJob: Job? = null

    init {
        // 从 DataStore 读取持久化设置
        viewModelScope.launch {
            store.data.collect { prefs ->
                prefs[KEY_LAYOUT]?.let {
                    _layoutMode.value = try { LayoutMode.valueOf(it) } catch (_: Exception) { LayoutMode.Honeycomb }
                }
                prefs[KEY_BLUR]?.let {
                    _blurEnabled.value = it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                }
                prefs[KEY_LOW_RES]?.let {
                    _lowResIcons.value = it
                    appRepository.refresh(if (it) 64 else 128)
                }
                prefs[KEY_SPLASH_ICON]?.let { _splashIcon.value = it }
                prefs[KEY_SPLASH_DELAY]?.let { _splashDelay.value = it.coerceIn(300, 1500) }
                prefs[KEY_ICON_PACK]?.let { pkg ->
                    if (pkg != _iconPack.value) {
                        _iconPack.value = pkg.ifEmpty { null }
                        appRepository.setIconPack(pkg.ifEmpty { null })
                    }
                }
                prefs[KEY_APP_ORDER]?.let { orderStr ->
                    val order = if (orderStr.isNotEmpty()) orderStr.split(",") else emptyList()
                    _appOrder.value = order
                    appRepository.setCustomOrder(order)
                }
            }
        }
    }

    fun setState(state: ScreenState) {
        _screenState.value = state
    }

    fun openApp(appInfo: AppInfo, origin: Offset = Offset(0.5f, 0.5f)) {
        _currentApp.value = appInfo
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App

        launchJob?.cancel()
        launchJob = viewModelScope.launch {
            delay(_splashDelay.value.toLong())
            launchingExternalApp = true
            appRepository.launchApp(appInfo)
        }
    }

    fun onReturnToLauncher() {
        if (launchingExternalApp) {
            launchingExternalApp = false
            _screenState.value = ScreenState.Apps
        }
    }

    fun handleHomePress() {
        when (_screenState.value) {
            ScreenState.Face -> _screenState.value = ScreenState.Apps
            ScreenState.Apps -> _screenState.value = ScreenState.Face
            ScreenState.App -> {
                launchJob?.cancel()
                launchJob = null
                launchingExternalApp = false
                _screenState.value = ScreenState.Apps
            }
            else -> _screenState.value = ScreenState.Face
        }
    }

    fun handleBackPress() {
        when (_screenState.value) {
            ScreenState.Face -> {}
            ScreenState.Apps -> _screenState.value = ScreenState.Face
            ScreenState.App -> {
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

    fun closeApp() { _screenState.value = ScreenState.Apps }
    fun goHome() { _screenState.value = ScreenState.Face }
    fun openSettings() { _screenState.value = ScreenState.Settings }

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        viewModelScope.launch { store.edit { it[KEY_LAYOUT] = mode.name } }
    }

    fun setBlurEnabled(enabled: Boolean) {
        val v = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        _blurEnabled.value = v
        viewModelScope.launch { store.edit { it[KEY_BLUR] = v } }
    }

    fun setLowResIcons(enabled: Boolean) {
        _lowResIcons.value = enabled
        appRepository.refresh(if (enabled) 64 else 128)
        viewModelScope.launch { store.edit { it[KEY_LOW_RES] = enabled } }
    }

    fun setSplashIcon(enabled: Boolean) {
        _splashIcon.value = enabled
        viewModelScope.launch { store.edit { it[KEY_SPLASH_ICON] = enabled } }
    }

    fun setSplashDelay(ms: Int) {
        _splashDelay.value = ms.coerceIn(300, 1500)
        viewModelScope.launch { store.edit { it[KEY_SPLASH_DELAY] = _splashDelay.value } }
    }

    fun setIconPack(packageName: String?) {
        _iconPack.value = packageName
        appRepository.setIconPack(packageName)
        viewModelScope.launch { store.edit { it[KEY_ICON_PACK] = packageName ?: "" } }
    }

    fun setAppOrder(order: List<String>) {
        _appOrder.value = order
        appRepository.setCustomOrder(order)
        viewModelScope.launch { store.edit { it[KEY_APP_ORDER] = order.joinToString(",") } }
    }

    fun swapApps(fromIndex: Int, toIndex: Int) {
        val current = _apps.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setAppOrder(current.map { it.packageName })
        }
    }

    fun getIconPackManager() = appRepository.getIconPackManager()

    private val _apps get() = appRepository.apps

    override fun onCleared() {
        super.onCleared()
        appRepository.destroy()
    }
}
