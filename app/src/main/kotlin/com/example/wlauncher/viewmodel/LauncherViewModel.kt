package com.example.wlauncher.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val KEY_LAYOUT = stringPreferencesKey("layout_mode")
        val KEY_BLUR = booleanPreferencesKey("blur_enabled")
        val KEY_EDGE_BLUR = booleanPreferencesKey("edge_blur_enabled")
        val KEY_LOW_RES = booleanPreferencesKey("low_res_icons")
        val KEY_ANIMATION_OVERRIDE = booleanPreferencesKey("animation_override_enabled")
        val KEY_SPLASH_ICON = booleanPreferencesKey("splash_icon")
        val KEY_SPLASH_DELAY = intPreferencesKey("splash_delay")
        val KEY_APP_ORDER = stringPreferencesKey("app_order")
        val KEY_LIST_ICON_SIZE = intPreferencesKey("list_icon_size")
        val KEY_HONEYCOMB_COLS = intPreferencesKey("honeycomb_cols")
        val KEY_HONEYCOMB_TOP_BLUR = intPreferencesKey("honeycomb_top_blur")
        val KEY_HONEYCOMB_BOTTOM_BLUR = intPreferencesKey("honeycomb_bottom_blur")
        val KEY_HONEYCOMB_TOP_FADE = intPreferencesKey("honeycomb_top_fade")
        val KEY_HONEYCOMB_BOTTOM_FADE = intPreferencesKey("honeycomb_bottom_fade")
        val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
    }

    private val store = application.dataStore
    private val appRepository = AppRepository(application)

    val apps: StateFlow<List<AppInfo>> = appRepository.apps

    private val _screenState = MutableStateFlow(ScreenState.Face)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.Honeycomb)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _blurEnabled = MutableStateFlow(true)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _edgeBlurEnabled = MutableStateFlow(false)
    val edgeBlurEnabled: StateFlow<Boolean> = _edgeBlurEnabled.asStateFlow()

    private val _lowResIcons = MutableStateFlow(false)
    val lowResIcons: StateFlow<Boolean> = _lowResIcons.asStateFlow()

    private val _animationOverrideEnabled = MutableStateFlow(true)
    val animationOverrideEnabled: StateFlow<Boolean> = _animationOverrideEnabled.asStateFlow()

    private val _splashDelay = MutableStateFlow(500)
    val splashDelay: StateFlow<Int> = _splashDelay.asStateFlow()

    private val _appOrder = MutableStateFlow<List<String>>(emptyList())
    val appOrder: StateFlow<List<String>> = _appOrder.asStateFlow()

    private val _listIconSize = MutableStateFlow(48)
    val listIconSize: StateFlow<Int> = _listIconSize.asStateFlow()

    private val _honeycombCols = MutableStateFlow(4)
    val honeycombCols: StateFlow<Int> = _honeycombCols.asStateFlow()

    private val _honeycombTopBlur = MutableStateFlow(12)
    val honeycombTopBlur: StateFlow<Int> = _honeycombTopBlur.asStateFlow()

    private val _honeycombBottomBlur = MutableStateFlow(12)
    val honeycombBottomBlur: StateFlow<Int> = _honeycombBottomBlur.asStateFlow()

    private val _honeycombTopFade = MutableStateFlow(56)
    val honeycombTopFade: StateFlow<Int> = _honeycombTopFade.asStateFlow()

    private val _honeycombBottomFade = MutableStateFlow(56)
    val honeycombBottomFade: StateFlow<Int> = _honeycombBottomFade.asStateFlow()

    private val _splashIcon = MutableStateFlow(true)
    val splashIcon: StateFlow<Boolean> = _splashIcon.asStateFlow()

    private val _showNotification = MutableStateFlow(true)
    val showNotification: StateFlow<Boolean> = _showNotification.asStateFlow()

    private val _appOpenOrigin = MutableStateFlow(Offset(0.5f, 0.5f))
    val appOpenOrigin: StateFlow<Offset> = _appOpenOrigin.asStateFlow()

    private val _currentApp = MutableStateFlow<AppInfo?>(null)
    val currentApp: StateFlow<AppInfo?> = _currentApp.asStateFlow()

    private var launchingExternalApp = false
    private var launchJob: Job? = null

    init {
        viewModelScope.launch {
            store.data.collect { prefs ->
                prefs[KEY_LAYOUT]?.let {
                    _layoutMode.value = try {
                        LayoutMode.valueOf(it)
                    } catch (_: Exception) {
                        LayoutMode.Honeycomb
                    }
                }
                prefs[KEY_BLUR]?.let { _blurEnabled.value = it }
                prefs[KEY_EDGE_BLUR]?.let { _edgeBlurEnabled.value = it }
                prefs[KEY_LOW_RES]?.let {
                    _lowResIcons.value = it
                    appRepository.refresh(if (it) 64 else 128)
                }
                prefs[KEY_ANIMATION_OVERRIDE]?.let { _animationOverrideEnabled.value = it }
                prefs[KEY_SPLASH_ICON]?.let { _splashIcon.value = it }
                prefs[KEY_SPLASH_DELAY]?.let { _splashDelay.value = it.coerceIn(300, 1500) }
                prefs[KEY_APP_ORDER]?.let { orderStr ->
                    val order = if (orderStr.isNotEmpty()) orderStr.split(",") else emptyList()
                    _appOrder.value = order
                    appRepository.setCustomOrder(order)
                }
                prefs[KEY_LIST_ICON_SIZE]?.let { _listIconSize.value = it.coerceIn(32, 80) }
                prefs[KEY_HONEYCOMB_COLS]?.let { _honeycombCols.value = it.coerceIn(3, 6) }
                prefs[KEY_HONEYCOMB_TOP_BLUR]?.let { _honeycombTopBlur.value = it.coerceIn(0, 48) }
                prefs[KEY_HONEYCOMB_BOTTOM_BLUR]?.let { _honeycombBottomBlur.value = it.coerceIn(0, 48) }
                prefs[KEY_HONEYCOMB_TOP_FADE]?.let { _honeycombTopFade.value = it.coerceIn(0, 160) }
                prefs[KEY_HONEYCOMB_BOTTOM_FADE]?.let { _honeycombBottomFade.value = it.coerceIn(0, 160) }
                prefs[KEY_SHOW_NOTIFICATION]?.let { _showNotification.value = it }
            }
        }
    }

    fun setState(state: ScreenState) {
        _screenState.value = state
    }

    fun openApp(appInfo: AppInfo, origin: Offset = Offset(0.5f, 0.5f), launchDelayMs: Long = _splashDelay.value.toLong()) {
        _currentApp.value = appInfo
        _appOpenOrigin.value = origin
        _screenState.value = ScreenState.App

        launchJob?.cancel()
        launchJob = viewModelScope.launch {
            delay(launchDelayMs)
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
            ScreenState.Face -> Unit
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

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
        viewModelScope.launch { store.edit { it[KEY_LAYOUT] = mode.name } }
    }

    fun setBlurEnabled(enabled: Boolean) {
        _blurEnabled.value = enabled
        if (!enabled) _edgeBlurEnabled.value = false
        viewModelScope.launch {
            store.edit {
                it[KEY_BLUR] = enabled
                if (!enabled) it[KEY_EDGE_BLUR] = false
            }
        }
    }

    fun setEdgeBlurEnabled(enabled: Boolean) {
        val value = enabled && _blurEnabled.value
        _edgeBlurEnabled.value = value
        viewModelScope.launch { store.edit { it[KEY_EDGE_BLUR] = value } }
    }

    fun setLowResIcons(enabled: Boolean) {
        _lowResIcons.value = enabled
        appRepository.refresh(if (enabled) 64 else 128)
        viewModelScope.launch { store.edit { it[KEY_LOW_RES] = enabled } }
    }

    fun setAnimationOverrideEnabled(enabled: Boolean) {
        _animationOverrideEnabled.value = enabled
        viewModelScope.launch { store.edit { it[KEY_ANIMATION_OVERRIDE] = enabled } }
    }

    fun setSplashIcon(enabled: Boolean) {
        _splashIcon.value = enabled
        viewModelScope.launch { store.edit { it[KEY_SPLASH_ICON] = enabled } }
    }

    fun setSplashDelay(ms: Int) {
        _splashDelay.value = ms.coerceIn(300, 1500)
        viewModelScope.launch { store.edit { it[KEY_SPLASH_DELAY] = _splashDelay.value } }
    }

    fun setAppOrder(order: List<String>) {
        _appOrder.value = order
        appRepository.setCustomOrder(order)
        viewModelScope.launch { store.edit { it[KEY_APP_ORDER] = order.joinToString(",") } }
    }

    fun setListIconSize(size: Int) {
        _listIconSize.value = size.coerceIn(32, 80)
        viewModelScope.launch { store.edit { it[KEY_LIST_ICON_SIZE] = _listIconSize.value } }
    }

    fun setHoneycombCols(cols: Int) {
        _honeycombCols.value = cols.coerceIn(3, 6)
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_COLS] = _honeycombCols.value } }
    }

    fun setHoneycombTopBlur(value: Int) {
        _honeycombTopBlur.value = value.coerceIn(0, 48)
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_TOP_BLUR] = _honeycombTopBlur.value } }
    }

    fun setHoneycombBottomBlur(value: Int) {
        _honeycombBottomBlur.value = value.coerceIn(0, 48)
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_BOTTOM_BLUR] = _honeycombBottomBlur.value } }
    }

    fun setHoneycombTopFade(value: Int) {
        _honeycombTopFade.value = value.coerceIn(0, 160)
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_TOP_FADE] = _honeycombTopFade.value } }
    }

    fun setHoneycombBottomFade(value: Int) {
        _honeycombBottomFade.value = value.coerceIn(0, 160)
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_BOTTOM_FADE] = _honeycombBottomFade.value } }
    }

    fun setShowNotification(show: Boolean) {
        _showNotification.value = show
        viewModelScope.launch { store.edit { it[KEY_SHOW_NOTIFICATION] = show } }
    }

    fun swapApps(fromIndex: Int, toIndex: Int) {
        val current = apps.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setAppOrder(current.map { it.packageName })
        }
    }

    fun resetSettings() {
        _layoutMode.value = LayoutMode.Honeycomb
        _blurEnabled.value = true
        _edgeBlurEnabled.value = false
        _lowResIcons.value = false
        _animationOverrideEnabled.value = true
        _splashIcon.value = true
        _splashDelay.value = 500
        _listIconSize.value = 48
        _honeycombCols.value = 4
        _honeycombTopBlur.value = 12
        _honeycombBottomBlur.value = 12
        _honeycombTopFade.value = 56
        _honeycombBottomFade.value = 56
        _showNotification.value = true
        appRepository.refresh(128)
        viewModelScope.launch {
            store.edit {
                it[KEY_LAYOUT] = LayoutMode.Honeycomb.name
                it[KEY_BLUR] = true
                it[KEY_EDGE_BLUR] = false
                it[KEY_LOW_RES] = false
                it[KEY_ANIMATION_OVERRIDE] = true
                it[KEY_SPLASH_ICON] = true
                it[KEY_SPLASH_DELAY] = 500
                it[KEY_LIST_ICON_SIZE] = 48
                it[KEY_HONEYCOMB_COLS] = 4
                it[KEY_HONEYCOMB_TOP_BLUR] = 12
                it[KEY_HONEYCOMB_BOTTOM_BLUR] = 12
                it[KEY_HONEYCOMB_TOP_FADE] = 56
                it[KEY_HONEYCOMB_BOTTOM_FADE] = 56
                it[KEY_SHOW_NOTIFICATION] = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appRepository.destroy()
    }
}
