package com.example.wlauncher.ui.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun GestureHost(
    screenState: ScreenState,
    onStateChange: (ScreenState) -> Unit,
    modifier: Modifier = Modifier,
    showNotification: Boolean = true,
    showControlCenter: Boolean = true,
    content: @Composable () -> Unit
) {
    var totalDx by remember { mutableFloatStateOf(0f) }
    var totalDy by remember { mutableFloatStateOf(0f) }
    var consumed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(screenState) {
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                        consumed = false
                    },
                    onDrag = { change, dragAmount ->
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y

                        if (!consumed && (abs(totalDx) > 80 || abs(totalDy) > 80)) {
                            consumed = true
                            val isVertical = abs(totalDy) > abs(totalDx)
                            val isHorizontal = !isVertical

                            when (screenState) {
                                ScreenState.Face -> {
                                    if (isVertical && totalDy < -80) {
                                        onStateChange(ScreenState.Apps)
                                        change.consume()
                                    } else if (isVertical && totalDy > 80 && showNotification) {
                                        onStateChange(ScreenState.Notifications)
                                        change.consume()
                                    } else if (isHorizontal && totalDx < -80 && showControlCenter) {
                                        // 左滑 → 控制中心
                                        onStateChange(ScreenState.ControlCenter)
                                        change.consume()
                                    }
                                }

                                ScreenState.Notifications -> {
                                    if (isVertical && totalDy < -80) {
                                        onStateChange(ScreenState.Face)
                                        change.consume()
                                    }
                                }

                                ScreenState.ControlCenter -> {
                                    if (isHorizontal && totalDx > 80) {
                                        // 右滑返回表盘
                                        onStateChange(ScreenState.Face)
                                        change.consume()
                                    }
                                }

                                ScreenState.Apps -> { /* 留给子层 */ }
                                ScreenState.App -> { /* 不拦截 */ }
                                ScreenState.Settings -> { /* 不拦截 */ }
                                ScreenState.Stack -> {
                                    onStateChange(ScreenState.Apps)
                                }
                            }
                        }
                    },
                    onDragEnd = {}
                )
            }
    ) {
        content()
    }
}
