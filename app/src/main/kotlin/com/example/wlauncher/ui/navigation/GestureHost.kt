package com.example.wlauncher.ui.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * 手势容器：在各状态间通过滑动手势导航。
 *
 * 表盘:
 *   - 上滑 → Smart Stack
 *   - 下滑 → 通知中心
 *   - 主页键进入应用列表 (不在这里处理)
 *
 * Smart Stack:
 *   - 继续上滑 → App列表
 *   - 下滑 → 回表盘
 *
 * 通知中心:
 *   - 上滑 → 回表盘
 *
 * 控制中心:
 *   - 任意滑动 → 回表盘
 *
 * App列表:
 *   - (不拦截，留给子层处理)
 */
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
                                        onStateChange(ScreenState.Stack)
                                        change.consume()
                                    } else if (isVertical && totalDy > 80) {
                                        onStateChange(ScreenState.Notifications)
                                        change.consume()
                                    }
                                }

                                ScreenState.Stack -> {
                                    if (isVertical && totalDy < -80) {
                                        onStateChange(ScreenState.Apps)
                                        change.consume()
                                    } else if (isVertical && totalDy > 80) {
                                        onStateChange(ScreenState.Face)
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
                                    // 不拦截，留给 HoneycombScreen/ListDrawer 处理
                                }

                                ScreenState.App -> {
                                    // App 状态不拦截手势
                                }

                                ScreenState.Settings -> {
                                    // 设置页不拦截手势
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
