package com.example.wlauncher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.example.wlauncher.ui.anim.platformBlur

@Composable
internal fun DrawerTopBlurMask(
    height: Dp,
    blurRadiusDp: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled || height.value <= 0f) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .platformBlur(blurRadiusDp.toFloat(), true)
            .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
    )
}

@Composable
internal fun DrawerBottomBlurMask(
    height: Dp,
    blurRadiusDp: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled || height.value <= 0f) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .platformBlur(blurRadiusDp.toFloat(), true)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
    )
}
