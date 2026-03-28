package com.example.wlauncher.config

import com.example.wlauncher.ui.navigation.LayoutMode

object LauncherDefaults {
    const val blurRadiusDp = 4
    const val splashDelayMs = 500
    const val appOpenAnimationDurationMs = 280
    const val appReturnAnimationDurationMs = 220
    const val listIconSizeDp = 48
    const val honeycombCols = 4
    const val honeycombFadeDp = 56
}

enum class IconScalePreset(
    val storageValue: String,
    val listIconSizeDp: Int,
    val cacheSizePx: Int,
    val scaleMultiplier: Float
) {
    AUTO("AUTO", LauncherDefaults.listIconSizeDp, 224, 1f),
    COMPACT("COMPACT", 44, 192, 0.88f),
    STANDARD("STANDARD", 48, 224, 1f),
    LARGE("LARGE", 54, 256, 1.12f),
    XLARGE("XLARGE", 60, 288, 1.24f),
    CUSTOM("CUSTOM", LauncherDefaults.listIconSizeDp, 224, 1f);

    companion object {
        fun fromStorage(value: String?): IconScalePreset {
            val normalized = value?.uppercase() ?: AUTO.storageValue
            return entries.firstOrNull { it.storageValue == normalized } ?: AUTO
        }
    }
}

fun IconScalePreset.resolveScaleMultiplier(smallestWidthDp: Int): Float {
    if (this != IconScalePreset.AUTO) return scaleMultiplier
    return when {
        smallestWidthDp >= 280 -> IconScalePreset.XLARGE.scaleMultiplier
        smallestWidthDp >= 240 -> IconScalePreset.LARGE.scaleMultiplier
        smallestWidthDp >= 200 -> IconScalePreset.STANDARD.scaleMultiplier
        else -> IconScalePreset.COMPACT.scaleMultiplier
    }
}

fun recommendedAutoListIconSizeDp(smallestWidthDp: Int): Int {
    return when {
        smallestWidthDp <= 176 -> 42
        smallestWidthDp <= 192 -> 46
        smallestWidthDp <= 208 -> 48
        smallestWidthDp <= 224 -> 52
        else -> 56
    }
}

fun recommendedAutoCacheSizePx(smallestWidthDp: Int): Int {
    return when {
        smallestWidthDp <= 176 -> 192
        smallestWidthDp <= 208 -> 224
        smallestWidthDp <= 240 -> 256
        else -> 288
    }
}

data class BlurConfig(
    val appLaunchBlurEnabled: Boolean = true,
    val edgeGradientBlurEnabled: Boolean = false,
    val menuBackgroundBlurEnabled: Boolean = true,
    val defaultRadiusDp: Int = LauncherDefaults.blurRadiusDp
) {
    val anyEnabled: Boolean
        get() = appLaunchBlurEnabled || edgeGradientBlurEnabled || menuBackgroundBlurEnabled
}

data class AnimationConfig(
    val systemAnimationOverrideEnabled: Boolean = true,
    val splashIconEnabled: Boolean = true,
    val splashDelayMs: Int = LauncherDefaults.splashDelayMs,
    val appOpenAnimationDurationMs: Int = LauncherDefaults.appOpenAnimationDurationMs,
    val appReturnAnimationDurationMs: Int = LauncherDefaults.appReturnAnimationDurationMs
)

data class IconConfig(
    val lowResIconsEnabled: Boolean = false,
    val autoIconSizeEnabled: Boolean = true,
    val iconScalePreset: IconScalePreset = IconScalePreset.AUTO,
    val listIconSizeDp: Int = LauncherDefaults.listIconSizeDp,
    val iconCacheSizePx: Int = 224
)

data class LayoutConfig(
    val mode: LayoutMode = LayoutMode.Honeycomb,
    val honeycombCols: Int = LauncherDefaults.honeycombCols,
    val honeycombTopFadeDp: Int = LauncherDefaults.honeycombFadeDp,
    val honeycombBottomFadeDp: Int = LauncherDefaults.honeycombFadeDp
)

data class LauncherConfig(
    val blur: BlurConfig = BlurConfig(),
    val animation: AnimationConfig = AnimationConfig(),
    val icon: IconConfig = IconConfig(),
    val layout: LayoutConfig = LayoutConfig(),
    val showNotification: Boolean = true
)
