package com.example.wlauncher.ui.drawer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.awaitFirstDown
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val DRAWER_DRAG_ARM_MS = 500L
internal const val DRAWER_MENU_TRIGGER_MS = 1000L
internal const val DRAWER_MENU_TO_DRAG_MS = 1500L

internal data class DrawerEdgeAutoScrollSpec(
    val viewportHeight: Float,
    val threshold: Float,
    val maxStep: Float
)

internal fun edgeAutoScrollDelta(
    pointerY: Float,
    spec: DrawerEdgeAutoScrollSpec
): Float {
    if (spec.threshold <= 0f || spec.maxStep <= 0f) return 0f
    return when {
        pointerY < spec.threshold -> {
            val progress = (1f - pointerY / spec.threshold).coerceIn(0f, 1f)
            -spec.maxStep * progress * progress
        }
        pointerY > spec.viewportHeight - spec.threshold -> {
            val progress = (1f - (spec.viewportHeight - pointerY) / spec.threshold).coerceIn(0f, 1f)
            spec.maxStep * progress * progress
        }
        else -> 0f
    }
}

internal suspend fun AwaitPointerEventScope.runDrawerLongPressSequence(
    touchSlop: Float,
    onShowMenu: () -> Unit,
    onMenuToDrag: () -> Unit,
    onBeginDrag: () -> Unit,
    onDragDelta: (Offset, Offset) -> Unit,
    onFinishDrag: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var dragArmed = false
        var menuShown = false
        var dragStarted = false
        var stillPressed = true
        var totalMovement = Offset.Zero

        coroutineScope {
            val armJob = launch {
                delay(DRAWER_DRAG_ARM_MS)
                if (stillPressed) {
                    dragArmed = true
                }
            }
            val menuJob = launch {
                delay(DRAWER_MENU_TRIGGER_MS)
                if (stillPressed && !dragStarted && totalMovement.getDistance() < touchSlop) {
                    menuShown = true
                    onShowMenu()
                }
            }
            val menuToDragJob = launch {
                delay(DRAWER_MENU_TO_DRAG_MS)
                if (stillPressed && menuShown && !dragStarted) {
                    onMenuToDrag()
                    dragStarted = true
                }
            }

            while (stillPressed) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                if (!change.pressed) {
                    stillPressed = false
                    break
                }
                val delta = change.position - change.previousPosition
                totalMovement += delta
                if (!menuShown && !dragStarted && dragArmed && totalMovement.getDistance() > touchSlop) {
                    onBeginDrag()
                    dragStarted = true
                }
                if (dragStarted) {
                    change.consume()
                    onDragDelta(delta, change.position)
                }
            }

            armJob.cancel()
            menuJob.cancel()
            menuToDragJob.cancel()
        }

        if (dragStarted) {
            onFinishDrag()
        }
    }
}
