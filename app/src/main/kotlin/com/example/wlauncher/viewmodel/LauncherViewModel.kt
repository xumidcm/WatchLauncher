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
import com.example.wlauncher.service.StepCounterManager
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
        val KEY_LIST_ICON_SIZE = intPreferencesKey("list_icon_size")
        val KEY_HONEYCOMB_COLS = intPreferencesKey("honeycomb_cols")
        val KEY_STEP_GOAL = intPreferencesKey("step_goal")
        val KEY_SHOW_STEPS = booleanPreferencesKey("show_steps")
        val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val KEY_SHOW_CONTROL_CENTER = booleanPreferencesKey("show_control_center")
        val KEY_FIRST_RUN = booleanPreferencesKey("first_run")
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

    private val _listIconSize = MutableStateFlow(48)
    val listIconSize: StateFlow<Int> = _listIconSize.asStateFlow()

    private val _honeycombCols = MutableStateFlow(4)
    val honeycombCols: StateFlow<Int> = _honeycombCols.asStateFlow()

    private val _splashIcon = MutableStateFlow(true)
    val splashIcon: StateFlow<Boolean> = _splashIcon.asStateFlow()

    private val _stepGoal = MutableStateFlow(10000)
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _showSteps = MutableStateFlow(true)
    val showSteps: StateFlow<Boolean> = _showSteps.asStateFlow()

    private val _showNotification = MutableStateFlow(true)
    val showNotification: StateFlow<Boolean> = _showNotification.asStateFlow()

    private val _showControlCenter = MutableStateFlow(true)
    val showControlCenter: StateFlow<Boolean> = _showControlCenter.asStateFlow()

    private val _firstRun = MutableStateFlow(true)
    val firstRun: StateFlow<Boolean> = _firstRun.asStateFlow()

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
                prefs[KEY_LIST_ICON_SIZE]?.let { _listIconSize.value = it.coerceIn(32, 80) }
                prefs[KEY_HONEYCOMB_COLS]?.let { _honeycombCols.value = it.coerceIn(3, 6) }
                prefs[KEY_STEP_GOAL]?.let {
                    _stepGoal.value = it.coerceIn(1000, 50000)
                    StepCounterManager.setGoal(it)
                }
                prefs[KEY_SHOW_STEPS]?.let { _showSteps.value = it }
                prefs[KEY_SHOW_NOTIFICATION]?.let { _showNotification.value = it }
                prefs[KEY_SHOW_CONTROL_CENTER]?.let { _showControlCenter.value = it }
                prefs[KEY_FIRST_RUN]?.let { _firstRun.value = it }
            }
        }

        // 初始化步数管理器
        StepCounterManager.initialize(application)
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

    fun setListIconSize(size: Int) {
        _listIconSize.value = size.coerceIn(32, 80)
        viewModelScope.launch { store.edit { it[KEY_LIST_ICON_SIZE] = _listIconSize.value } }
    }

    fun setHoneycombCols(cols: Int) {
        _honeycombCols.value = cols.coerceIn(3, 6)
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_COLS] = _honeycombCols.value } }
    }

    fun setStepGoal(goal: Int) {
        _stepGoal.value = goal.coerceIn(1000, 50000)
        StepCounterManager.setGoal(_stepGoal.value)
        viewModelScope.launch { store.edit { it[KEY_STEP_GOAL] = _stepGoal.value } }
    }

    fun setShowSteps(show: Boolean) {
        _showSteps.value = show
        viewModelScope.launch { store.edit { it[KEY_SHOW_STEPS] = show } }
    }

    fun setShowNotification(show: Boolean) {
        _showNotification.value = show
        viewModelScope.launch { store.edit { it[KEY_SHOW_NOTIFICATION] = show } }
    }

    fun setShowControlCenter(show: Boolean) {
        _showControlCenter.value = show
        viewModelScope.launch { store.edit { it[KEY_SHOW_CONTROL_CENTER] = show } }
    }

    fun setFirstRun(first: Boolean) {
        _firstRun.value = first
        viewModelScope.launch { store.edit { it[KEY_FIRST_RUN] = first } }
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
        StepCounterManager.release()
    }
}
