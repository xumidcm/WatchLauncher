package com.example.wlauncher.ui.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppBubble(
    icon: ImageBitmap,
    size: Dp = 54.dp,
    pressed: Boolean = false,
    scaleTargetWhenPressed: Float = 0.94f,
    animationDurationMillis: Int = 180,
    modifier: Modifier = Modifier
) {
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) scaleTargetWhenPressed else 1f,
        animationSpec = tween(durationMillis = animationDurationMillis),
        label = "bubble_scale"
    )
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.14f else 0f,
        animationSpec = tween(durationMillis = animationDurationMillis),
        label = "bubble_overlay"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer {
                shape = CircleShape
                clip = true
                scaleX = pressedScale
                scaleY = pressedScale
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        if (pressedOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = pressedOverlayAlpha))
            )
        }
    }
}
