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

                            when (screenState) {
                                ScreenState.Face -> {
                                    if (isVertical && totalDy < -80) {
                                        // 上滑直接进应用列表
                                        onStateChange(ScreenState.Apps)
                                        change.consume()
                                    } else if (isVertical && totalDy > 80) {
                                        onStateChange(ScreenState.Notifications)
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
                                    onStateChange(ScreenState.Face)
                                    change.consume()
                                }

                                ScreenState.Apps -> {
                                    // 不拦截，留给 HoneycombScreen/ListDrawer 处理滚动
                                    // 上滑返回由子层在到达顶部时触发
                                }

                                ScreenState.App -> { /* 不拦截 */ }
                                ScreenState.Settings -> { /* 不拦截 */ }
                                ScreenState.Stack -> {
                                    // Stack 已废弃，自动跳到 Apps
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
