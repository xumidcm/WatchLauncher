package com.example.wlauncher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppBubble(
    icon: ImageBitmap,
    size: Dp = 54.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPressedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val pressOffsetPx = with(density) { size.toPx() * 0.08f }

    LaunchedEffect(isPressed) {
        onPressedChange(isPressed)
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val pressedScale = if (isPressed) 0.92f else 1f
                shadowElevation = 8.dp.toPx()
                shape = CircleShape
                clip = true
                scaleX = pressedScale
                scaleY = pressedScale
                translationY = if (isPressed) pressOffsetPx else 0f
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Crop
        )
    }
}
