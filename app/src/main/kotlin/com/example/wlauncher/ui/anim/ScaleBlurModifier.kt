package com.example.wlauncher.ui.anim

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/**
 * 组合 Modifier：scale + blur + opacity + translationY。
 * 1:1 复刻 HTML 原型中各层的 CSS transition 效果。
 *
 * @param targetValues 目标动画值
 * @param screenHeight 屏幕高度像素，用于 translationY 计算
 * @param blurEnabled 是否启用模糊（API 31+ 才支持，低版本自动跳过）
 */
@Composable
fun Modifier.scaleBlurAlpha(
    targetValues: LayerAnimValues,
    screenHeight: Float = 0f,
    blurEnabled: Boolean = true
): Modifier {
    val animScale = remember { Animatable(targetValues.scale) }
    val animAlpha = remember { Animatable(targetValues.alpha) }
    val animBlur = remember { Animatable(targetValues.blur) }
    val animTransY = remember { Animatable(targetValues.translationY) }

    LaunchedEffect(targetValues) {
        kotlinx.coroutines.launch { animScale.animateTo(targetValues.scale, TransitionSpring) }
        kotlinx.coroutines.launch { animAlpha.animateTo(targetValues.alpha, AlphaSpec) }
        kotlinx.coroutines.launch { animBlur.animateTo(targetValues.blur, AlphaSpec) }
        kotlinx.coroutines.launch { animTransY.animateTo(targetValues.translationY, TransitionSpring) }
    }

    val density = LocalDensity.current

    return this.graphicsLayer {
        scaleX = animScale.value
        scaleY = animScale.value
        alpha = animAlpha.value
        translationY = animTransY.value * screenHeight

        // 实时高斯模糊 (API 31+, 且用户开启)
        val blurPx = animBlur.value * density.density
        if (blurEnabled && blurPx > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            renderEffect = RenderEffect.createBlurEffect(
                blurPx, blurPx, Shader.TileMode.CLAMP
            )
        } else {
            renderEffect = null
        }
    }
}
