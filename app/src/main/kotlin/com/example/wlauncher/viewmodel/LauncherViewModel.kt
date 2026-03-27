package com.example.wlauncher.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wlauncher.config.AnimationConfig
import com.example.wlauncher.config.BlurConfig
import com.example.wlauncher.config.IconConfig
import com.example.wlauncher.config.IconScalePreset
import com.example.wlauncher.config.LauncherConfig
import com.example.wlauncher.config.LauncherDefaults
import com.example.wlauncher.config.LayoutConfig
import com.example.wlauncher.config.recommendedAutoCacheSizePx
import com.example.wlauncher.config.recommendedAutoListIconSizeDp
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
        val KEY_APP_TRANSITION_BLUR = booleanPreferencesKey("app_transition_blur_enabled")
        val KEY_EDGE_GRADIENT_BLUR = booleanPreferencesKey("edge_gradient_blur_enabled")
        val KEY_MENU_BG_BLUR = booleanPreferencesKey("menu_background_blur_enabled")
        val KEY_BLUR_RADIUS = intPreferencesKey("blur_radius_dp")

        val KEY_LOW_RES = booleanPreferencesKey("low_res_icons")
        val KEY_ICON_SIZE_AUTO = booleanPreferencesKey("icon_size_auto")
        val KEY_ICON_SIZE_PRESET = stringPreferencesKey("icon_size_preset")
        val KEY_LIST_ICON_SIZE = intPreferencesKey("list_icon_size")

        val KEY_ANIMATION_OVERRIDE = booleanPreferencesKey("animation_override_enabled")
        val KEY_SPLASH_ICON = booleanPreferencesKey("splash_icon")
        val KEY_SPLASH_DELAY = intPreferencesKey("splash_delay")
        val KEY_APP_OPEN_ANIM_DURATION = intPreferencesKey("app_open_anim_duration")
        val KEY_APP_RETURN_ANIM_DURATION = intPreferencesKey("app_return_anim_duration")

        val KEY_APP_ORDER = stringPreferencesKey("app_order")
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

    private val _appLaunchBlurEnabled = MutableStateFlow(true)
    val appLaunchBlurEnabled: StateFlow<Boolean> = _appLaunchBlurEnabled.asStateFlow()

    private val _edgeBlurEnabled = MutableStateFlow(false)
    val edgeBlurEnabled: StateFlow<Boolean> = _edgeBlurEnabled.asStateFlow()

    private val _menuBackgroundBlurEnabled = MutableStateFlow(true)
    val menuBackgroundBlurEnabled: StateFlow<Boolean> = _menuBackgroundBlurEnabled.asStateFlow()

    private val _blurRadiusDp = MutableStateFlow(LauncherDefaults.blurRadiusDp)
    val blurRadiusDp: StateFlow<Int> = _blurRadiusDp.asStateFlow()

    private val _lowResIcons = MutableStateFlow(false)
    val lowResIcons: StateFlow<Boolean> = _lowResIcons.asStateFlow()

    private val _iconSizeAuto = MutableStateFlow(true)
    val iconSizeAuto: StateFlow<Boolean> = _iconSizeAuto.asStateFlow()

    private val _iconSizePreset = MutableStateFlow(IconScalePreset.AUTO.storageValue)
    val iconSizePreset: StateFlow<String> = _iconSizePreset.asStateFlow()

    private val _listIconSize = MutableStateFlow(LauncherDefaults.listIconSizeDp)
    val listIconSize: StateFlow<Int> = _listIconSize.asStateFlow()

    private val _animationOverrideEnabled = MutableStateFlow(true)
    val animationOverrideEnabled: StateFlow<Boolean> = _animationOverrideEnabled.asStateFlow()

    private val _splashIcon = MutableStateFlow(true)
    val splashIcon: StateFlow<Boolean> = _splashIcon.asStateFlow()

    private val _splashDelay = MutableStateFlow(LauncherDefaults.splashDelayMs)
    val splashDelay: StateFlow<Int> = _splashDelay.asStateFlow()

    private val _appOpenAnimationDuration = MutableStateFlow(LauncherDefaults.appOpenAnimationDurationMs)
    val appOpenAnimationDuration: StateFlow<Int> = _appOpenAnimationDuration.asStateFlow()

    private val _appReturnAnimationDuration = MutableStateFlow(LauncherDefaults.appReturnAnimationDurationMs)
    val appReturnAnimationDuration: StateFlow<Int> = _appReturnAnimationDuration.asStateFlow()

    private val _appOrder = MutableStateFlow<List<String>>(emptyList())
    val appOrder: StateFlow<List<String>> = _appOrder.asStateFlow()

    private val _honeycombCols = MutableStateFlow(LauncherDefaults.honeycombCols)
    val honeycombCols: StateFlow<Int> = _honeycombCols.asStateFlow()

    private val _honeycombTopBlur = MutableStateFlow(LauncherDefaults.blurRadiusDp)
    val honeycombTopBlur: StateFlow<Int> = _honeycombTopBlur.asStateFlow()

    private val _honeycombBottomBlur = MutableStateFlow(LauncherDefaults.blurRadiusDp)
    val honeycombBottomBlur: StateFlow<Int> = _honeycombBottomBlur.asStateFlow()

    private val _honeycombTopFade = MutableStateFlow(LauncherDefaults.honeycombFadeDp)
    val honeycombTopFade: StateFlow<Int> = _honeycombTopFade.asStateFlow()

    private val _honeycombBottomFade = MutableStateFlow(LauncherDefaults.honeycombFadeDp)
    val honeycombBottomFade: StateFlow<Int> = _honeycombBottomFade.asStateFlow()

    private val _showNotification = MutableStateFlow(true)
    val showNotification: StateFlow<Boolean> = _showNotification.asStateFlow()

    private val _uiConfig = MutableStateFlow(LauncherConfig())
    val uiConfig: StateFlow<LauncherConfig> = _uiConfig.asStateFlow()

    private val _appOpenOrigin = MutableStateFlow(Offset(0.5f, 0.5f))
    val appOpenOrigin: StateFlow<Offset> = _appOpenOrigin.asStateFlow()

    private val _currentApp = MutableStateFlow<AppInfo?>(null)
    val currentApp: StateFlow<AppInfo?> = _currentApp.asStateFlow()

    private var launchingExternalApp = false
    private var launchJob: Job? = null

    init {
        viewModelScope.launch {
            store.data.collect { prefs ->
                applyPrefs(prefs)
            }
        }
    }

    private fun applyPrefs(prefs: Preferences) {
        _layoutMode.value = prefs[KEY_LAYOUT]?.let {
            try {
                LayoutMode.valueOf(it)
            } catch (_: Exception) {
                LayoutMode.Honeycomb
            }
        } ?: LayoutMode.Honeycomb

        val legacyBlur = prefs[KEY_BLUR]
        _appLaunchBlurEnabled.value = prefs[KEY_APP_TRANSITION_BLUR] ?: legacyBlur ?: true
        _edgeBlurEnabled.value = prefs[KEY_EDGE_GRADIENT_BLUR] ?: prefs[KEY_EDGE_BLUR] ?: false
        _menuBackgroundBlurEnabled.value = prefs[KEY_MENU_BG_BLUR] ?: legacyBlur ?: true
        _blurRadiusDp.value = (prefs[KEY_BLUR_RADIUS] ?: LauncherDefaults.blurRadiusDp).coerceIn(0, 48)
        updateDerivedBlurEnabled()

        _lowResIcons.value = prefs[KEY_LOW_RES] ?: false
        _iconSizeAuto.value = prefs[KEY_ICON_SIZE_AUTO] ?: true
        _iconSizePreset.value = normalizePreset(prefs[KEY_ICON_SIZE_PRESET])
        _listIconSize.value = resolveListIconSize(
            auto = _iconSizeAuto.value,
            presetName = _iconSizePreset.value,
            manualSize = prefs[KEY_LIST_ICON_SIZE]
        )

        _animationOverrideEnabled.value = prefs[KEY_ANIMATION_OVERRIDE] ?: true
        _splashIcon.value = prefs[KEY_SPLASH_ICON] ?: true
        _splashDelay.value = (prefs[KEY_SPLASH_DELAY] ?: LauncherDefaults.splashDelayMs).coerceIn(300, 1500)
        _appOpenAnimationDuration.value =
            (prefs[KEY_APP_OPEN_ANIM_DURATION] ?: LauncherDefaults.appOpenAnimationDurationMs).coerceIn(120, 1200)
        _appReturnAnimationDuration.value =
            (prefs[KEY_APP_RETURN_ANIM_DURATION] ?: LauncherDefaults.appReturnAnimationDurationMs).coerceIn(120, 1200)

        val orderStr = prefs[KEY_APP_ORDER]
        val order = if (!orderStr.isNullOrEmpty()) orderStr.split(",") else emptyList()
        _appOrder.value = order
        appRepository.setCustomOrder(order)

        _honeycombCols.value = (prefs[KEY_HONEYCOMB_COLS] ?: LauncherDefaults.honeycombCols).coerceIn(3, 6)
        _honeycombTopBlur.value = (prefs[KEY_HONEYCOMB_TOP_BLUR] ?: LauncherDefaults.blurRadiusDp).coerceIn(0, 48)
        _honeycombBottomBlur.value = (prefs[KEY_HONEYCOMB_BOTTOM_BLUR] ?: LauncherDefaults.blurRadiusDp).coerceIn(0, 48)
        _honeycombTopFade.value = (prefs[KEY_HONEYCOMB_TOP_FADE] ?: LauncherDefaults.honeycombFadeDp).coerceIn(0, 160)
        _honeycombBottomFade.value = (prefs[KEY_HONEYCOMB_BOTTOM_FADE] ?: LauncherDefaults.honeycombFadeDp).coerceIn(0, 160)
        _showNotification.value = prefs[KEY_SHOW_NOTIFICATION] ?: true

        syncUiConfig()
        appRepository.refresh(resolveIconCacheSize())
    }

    private fun smallestWidthDp(): Int {
        val config = getApplication<Application>().resources.configuration
        return if (config.smallestScreenWidthDp != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) {
            config.smallestScreenWidthDp
        } else {
            192
        }
    }

    private fun normalizePreset(value: String?): String {
        return IconScalePreset.fromStorage(value).storageValue
    }

    private fun resolveListIconSize(auto: Boolean, presetName: String, manualSize: Int?): Int {
        if (auto || presetName == IconScalePreset.AUTO.storageValue) {
            return recommendedAutoListIconSizeDp(smallestWidthDp()).coerceIn(32, 80)
        }
        val preset = IconScalePreset.fromStorage(presetName)
        return if (preset == IconScalePreset.CUSTOM) {
            (manualSize ?: LauncherDefaults.listIconSizeDp).coerceIn(32, 80)
        } else {
            preset.listIconSizeDp.coerceIn(32, 80)
        }
    }

    private fun resolveIconCacheSize(): Int {
        if (_lowResIcons.value) return 96
        if (_iconSizeAuto.value || _iconSizePreset.value == IconScalePreset.AUTO.storageValue) {
            return recommendedAutoCacheSizePx(smallestWidthDp())
        }
        return IconScalePreset.fromStorage(_iconSizePreset.value).cacheSizePx
    }

    private fun updateDerivedBlurEnabled() {
        _blurEnabled.value = _appLaunchBlurEnabled.value || _edgeBlurEnabled.value || _menuBackgroundBlurEnabled.value
    }

    private fun syncUiConfig() {
        _uiConfig.value = LauncherConfig(
            blur = BlurConfig(
                appLaunchBlurEnabled = _appLaunchBlurEnabled.value,
                edgeGradientBlurEnabled = _edgeBlurEnabled.value,
                menuBackgroundBlurEnabled = _menuBackgroundBlurEnabled.value,
                defaultRadiusDp = _blurRadiusDp.value
            ),
            animation = AnimationConfig(
                systemAnimationOverrideEnabled = _animationOverrideEnabled.value,
                splashIconEnabled = _splashIcon.value,
                splashDelayMs = _splashDelay.value,
                appOpenAnimationDurationMs = _appOpenAnimationDuration.value,
                appReturnAnimationDurationMs = _appReturnAnimationDuration.value
            ),
            icon = IconConfig(
                lowResIconsEnabled = _lowResIcons.value,
                autoIconSizeEnabled = _iconSizeAuto.value,
                iconScalePreset = IconScalePreset.fromStorage(_iconSizePreset.value),
                listIconSizeDp = _listIconSize.value,
                iconCacheSizePx = resolveIconCacheSize()
            ),
            layout = LayoutConfig(
                mode = _layoutMode.value,
                honeycombCols = _honeycombCols.value,
                honeycombTopBlurDp = _honeycombTopBlur.value,
                honeycombBottomBlurDp = _honeycombBottomBlur.value,
                honeycombTopFadeDp = _honeycombTopFade.value,
                honeycombBottomFadeDp = _honeycombBottomFade.value
            ),
            showNotification = _showNotification.value
        )
    }

    private fun writeBlurPrefs(prefs: MutablePreferences) {
        prefs[KEY_APP_TRANSITION_BLUR] = _appLaunchBlurEnabled.value
        prefs[KEY_EDGE_GRADIENT_BLUR] = _edgeBlurEnabled.value
        prefs[KEY_MENU_BG_BLUR] = _menuBackgroundBlurEnabled.value
        prefs[KEY_BLUR_RADIUS] = _blurRadiusDp.value
        prefs[KEY_BLUR] = _blurEnabled.value
        prefs[KEY_EDGE_BLUR] = _edgeBlurEnabled.value
    }

    private fun writeIconPrefs(prefs: MutablePreferences) {
        prefs[KEY_LOW_RES] = _lowResIcons.value
        prefs[KEY_ICON_SIZE_AUTO] = _iconSizeAuto.value
        prefs[KEY_ICON_SIZE_PRESET] = _iconSizePreset.value
        prefs[KEY_LIST_ICON_SIZE] = _listIconSize.value
    }

    private fun updateIconState(auto: Boolean, presetName: String, manualSize: Int? = _listIconSize.value) {
        _iconSizeAuto.value = auto
        _iconSizePreset.value = normalizePreset(presetName)
        _listIconSize.value = resolveListIconSize(auto, _iconSizePreset.value, manualSize)
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
            launchingExternalApp = appRepository.launchApp(appInfo)
            if (!launchingExternalApp) {
                _currentApp.value = null
                _screenState.value = ScreenState.Apps
            }
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
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_LAYOUT] = mode.name } }
    }

    fun setBlurEnabled(enabled: Boolean) {
        _appLaunchBlurEnabled.value = enabled
        _edgeBlurEnabled.value = enabled
        _menuBackgroundBlurEnabled.value = enabled
        updateDerivedBlurEnabled()
        syncUiConfig()
        viewModelScope.launch { store.edit { writeBlurPrefs(it) } }
    }

    fun setEdgeBlurEnabled(enabled: Boolean) {
        setEdgeGradientBlurEnabled(enabled)
    }

    fun setAppLaunchBlurEnabled(enabled: Boolean) {
        _appLaunchBlurEnabled.value = enabled
        updateDerivedBlurEnabled()
        syncUiConfig()
        viewModelScope.launch { store.edit { writeBlurPrefs(it) } }
    }

    fun setEdgeGradientBlurEnabled(enabled: Boolean) {
        _edgeBlurEnabled.value = enabled
        updateDerivedBlurEnabled()
        syncUiConfig()
        viewModelScope.launch { store.edit { writeBlurPrefs(it) } }
    }

    fun setMenuBackgroundBlurEnabled(enabled: Boolean) {
        _menuBackgroundBlurEnabled.value = enabled
        updateDerivedBlurEnabled()
        syncUiConfig()
        viewModelScope.launch { store.edit { writeBlurPrefs(it) } }
    }

    fun setBlurRadiusDp(value: Int) {
        _blurRadiusDp.value = value.coerceIn(0, 48)
        syncUiConfig()
        viewModelScope.launch { store.edit { writeBlurPrefs(it) } }
    }

    fun setLowResIcons(enabled: Boolean) {
        _lowResIcons.value = enabled
        syncUiConfig()
        appRepository.refresh(resolveIconCacheSize())
        viewModelScope.launch { store.edit { writeIconPrefs(it) } }
    }

    fun setIconSizeAuto(enabled: Boolean) {
        val nextPreset = when {
            enabled -> IconScalePreset.AUTO.storageValue
            _iconSizePreset.value == IconScalePreset.AUTO.storageValue -> IconScalePreset.STANDARD.storageValue
            else -> _iconSizePreset.value
        }
        updateIconState(auto = enabled, presetName = nextPreset)
        syncUiConfig()
        appRepository.refresh(resolveIconCacheSize())
        viewModelScope.launch { store.edit { writeIconPrefs(it) } }
    }

    fun setIconSizePreset(preset: String) {
        val normalized = normalizePreset(preset)
        if (normalized == IconScalePreset.AUTO.storageValue) {
            updateIconState(auto = true, presetName = normalized)
        } else {
            updateIconState(auto = false, presetName = normalized)
        }
        syncUiConfig()
        appRepository.refresh(resolveIconCacheSize())
        viewModelScope.launch { store.edit { writeIconPrefs(it) } }
    }

    fun setAnimationOverrideEnabled(enabled: Boolean) {
        _animationOverrideEnabled.value = enabled
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_ANIMATION_OVERRIDE] = enabled } }
    }

    fun setSplashIcon(enabled: Boolean) {
        _splashIcon.value = enabled
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_SPLASH_ICON] = enabled } }
    }

    fun setSplashDelay(ms: Int) {
        _splashDelay.value = ms.coerceIn(300, 1500)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_SPLASH_DELAY] = _splashDelay.value } }
    }

    fun setAppOpenAnimationDuration(ms: Int) {
        _appOpenAnimationDuration.value = ms.coerceIn(120, 1200)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_APP_OPEN_ANIM_DURATION] = _appOpenAnimationDuration.value } }
    }

    fun setAppReturnAnimationDuration(ms: Int) {
        _appReturnAnimationDuration.value = ms.coerceIn(120, 1200)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_APP_RETURN_ANIM_DURATION] = _appReturnAnimationDuration.value } }
    }

    fun setAppOrder(order: List<String>) {
        _appOrder.value = order
        appRepository.setCustomOrder(order)
        viewModelScope.launch { store.edit { it[KEY_APP_ORDER] = order.joinToString(",") } }
    }

    fun setListIconSize(size: Int) {
        _listIconSize.value = size.coerceIn(32, 80)
        _iconSizeAuto.value = false
        _iconSizePreset.value = IconScalePreset.CUSTOM.storageValue
        syncUiConfig()
        appRepository.refresh(resolveIconCacheSize())
        viewModelScope.launch { store.edit { writeIconPrefs(it) } }
    }

    fun setHoneycombCols(cols: Int) {
        _honeycombCols.value = cols.coerceIn(3, 6)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_COLS] = _honeycombCols.value } }
    }

    fun setHoneycombTopBlur(value: Int) {
        _honeycombTopBlur.value = value.coerceIn(0, 48)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_TOP_BLUR] = _honeycombTopBlur.value } }
    }

    fun setHoneycombBottomBlur(value: Int) {
        _honeycombBottomBlur.value = value.coerceIn(0, 48)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_BOTTOM_BLUR] = _honeycombBottomBlur.value } }
    }

    fun setHoneycombTopFade(value: Int) {
        _honeycombTopFade.value = value.coerceIn(0, 160)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_TOP_FADE] = _honeycombTopFade.value } }
    }

    fun setHoneycombBottomFade(value: Int) {
        _honeycombBottomFade.value = value.coerceIn(0, 160)
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_HONEYCOMB_BOTTOM_FADE] = _honeycombBottomFade.value } }
    }

    fun setShowNotification(show: Boolean) {
        _showNotification.value = show
        syncUiConfig()
        viewModelScope.launch { store.edit { it[KEY_SHOW_NOTIFICATION] = show } }
    }

    fun swapApps(fromIndex: Int, toIndex: Int) {
        val current = apps.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setAppOrder(current.map { it.componentKey })
        }
    }

    fun resetSettings() {
        _layoutMode.value = LayoutMode.Honeycomb

        _appLaunchBlurEnabled.value = true
        _edgeBlurEnabled.value = false
        _menuBackgroundBlurEnabled.value = true
        _blurRadiusDp.value = LauncherDefaults.blurRadiusDp
        updateDerivedBlurEnabled()

        _lowResIcons.value = false
        _iconSizeAuto.value = true
        _iconSizePreset.value = IconScalePreset.AUTO.storageValue
        _listIconSize.value = recommendedAutoListIconSizeDp(smallestWidthDp()).coerceIn(32, 80)

        _animationOverrideEnabled.value = true
        _splashIcon.value = true
        _splashDelay.value = LauncherDefaults.splashDelayMs
        _appOpenAnimationDuration.value = LauncherDefaults.appOpenAnimationDurationMs
        _appReturnAnimationDuration.value = LauncherDefaults.appReturnAnimationDurationMs

        _honeycombCols.value = LauncherDefaults.honeycombCols
        _honeycombTopBlur.value = LauncherDefaults.blurRadiusDp
        _honeycombBottomBlur.value = LauncherDefaults.blurRadiusDp
        _honeycombTopFade.value = LauncherDefaults.honeycombFadeDp
        _honeycombBottomFade.value = LauncherDefaults.honeycombFadeDp
        _showNotification.value = true

        syncUiConfig()
        appRepository.refresh(resolveIconCacheSize())
        viewModelScope.launch {
            store.edit { prefs ->
                prefs[KEY_LAYOUT] = LayoutMode.Honeycomb.name
                writeBlurPrefs(prefs)
                writeIconPrefs(prefs)
                prefs[KEY_ANIMATION_OVERRIDE] = true
                prefs[KEY_SPLASH_ICON] = true
                prefs[KEY_SPLASH_DELAY] = LauncherDefaults.splashDelayMs
                prefs[KEY_APP_OPEN_ANIM_DURATION] = LauncherDefaults.appOpenAnimationDurationMs
                prefs[KEY_APP_RETURN_ANIM_DURATION] = LauncherDefaults.appReturnAnimationDurationMs
                prefs[KEY_HONEYCOMB_COLS] = LauncherDefaults.honeycombCols
                prefs[KEY_HONEYCOMB_TOP_BLUR] = LauncherDefaults.blurRadiusDp
                prefs[KEY_HONEYCOMB_BOTTOM_BLUR] = LauncherDefaults.blurRadiusDp
                prefs[KEY_HONEYCOMB_TOP_FADE] = LauncherDefaults.honeycombFadeDp
                prefs[KEY_HONEYCOMB_BOTTOM_FADE] = LauncherDefaults.honeycombFadeDp
                prefs[KEY_SHOW_NOTIFICATION] = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appRepository.destroy()
    }
}
