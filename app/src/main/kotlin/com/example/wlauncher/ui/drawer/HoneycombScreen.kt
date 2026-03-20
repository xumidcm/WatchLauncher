package com.example.wlauncher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.theme.WatchColors
import com.example.wlauncher.util.fisheyeScale
import com.example.wlauncher.util.generateHexSpiral
import com.example.wlauncher.util.hexToPixel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HoneycombScreen(
    apps: List<AppInfo>,
    onAppClick: (AppInfo, Offset) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenCenter = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
        val screenRadius = minOf(screenWidthPx, screenHeightPx) / 2f
        val baseCellSize = with(density) { 34.dp.toPx() }
        val iconSizeDp = 50.dp
        val iconSizePx = with(density) { iconSizeDp.toPx() }
        val viewportPaddingPx = with(density) { 24.dp.toPx() }
        val bottomSafePaddingPx = with(density) { 88.dp.toPx() }
        val minZoom = 0.82f
        val maxZoom = 1.36f

        val hexPositions = remember(apps.size) { generateHexSpiral(apps.size) }
        var panOffset by remember { mutableStateOf(Offset.Zero) }
        var gridZoom by remember { mutableFloatStateOf(1f) }

        fun clampPan(offset: Offset, zoom: Float): Offset {
            val positions = hexPositions.map { hexToPixel(it, baseCellSize * zoom) }
            val maxContentX = positions.maxOfOrNull { abs(it.x) } ?: 0f
            val maxContentY = positions.maxOfOrNull { abs(it.y) } ?: 0f
            val horizontalReach = (screenWidthPx / 2f - viewportPaddingPx - iconSizePx / 2f).coerceAtLeast(0f)
            val verticalReach = (
                screenHeightPx / 2f - viewportPaddingPx - iconSizePx / 2f - bottomSafePaddingPx / 2f
            ).coerceAtLeast(0f)
            val maxPanX = (maxContentX - horizontalReach).coerceAtLeast(0f)
            val maxPanY = (maxContentY - verticalReach).coerceAtLeast(0f)
            return Offset(
                x = offset.x.coerceIn(-maxPanX, maxPanX),
                y = offset.y.coerceIn(-maxPanY, maxPanY)
            )
        }

        LaunchedEffect(apps.size, gridZoom, screenWidthPx, screenHeightPx) {
            panOffset = clampPan(panOffset, gridZoom)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(apps.size) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val nextZoom = (gridZoom * zoomChange).coerceIn(minZoom, maxZoom)
                        gridZoom = nextZoom
                        panOffset = clampPan(panOffset + pan, nextZoom)
                    }
                }
        ) {
            apps.forEachIndexed { index, app ->
                if (index >= hexPositions.size) return@forEachIndexed

                val hexPixel = hexToPixel(hexPositions[index], baseCellSize * gridZoom)
                val worldPos = hexPixel + panOffset
                val screenPos = screenCenter + worldPos
                val distFromCenter = worldPos.getDistance()
                val bubbleScale = (
                    fisheyeScale(
                        distance = distFromCenter,
                        maxDistance = screenRadius,
                        maxScale = 1.08f,
                        minScale = 0.62f
                    ) * gridZoom
                ).coerceIn(0.56f, 1.5f)
                val renderedHalfSize = iconSizePx * bubbleScale / 2f

                if (
                    screenPos.x < -renderedHalfSize ||
                    screenPos.x > screenWidthPx + renderedHalfSize ||
                    screenPos.y < -renderedHalfSize ||
                    screenPos.y > screenHeightPx + renderedHalfSize
                ) {
                    return@forEachIndexed
                }

                AppBubble(
                    icon = app.icon,
                    size = iconSizeDp,
                    onClick = {
                        val normalizedOrigin = Offset(
                            x = (screenPos.x / screenWidthPx).coerceIn(0f, 1f),
                            y = (screenPos.y / screenHeightPx).coerceIn(0f, 1f)
                        )
                        onAppClick(app, normalizedOrigin)
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (screenPos.x - iconSizePx / 2f).roundToInt(),
                                y = (screenPos.y - iconSizePx / 2f).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            scaleX = bubbleScale
                            scaleY = bubbleScale
                            alpha = bubbleScale.coerceIn(0.52f, 1f)
                        }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp)
                    .size(42.dp)
                    .graphicsLayer {
                        shadowElevation = 12.dp.toPx()
                        shape = CircleShape
                        clip = true
                    }
                    .background(WatchColors.SurfaceGlass)
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
