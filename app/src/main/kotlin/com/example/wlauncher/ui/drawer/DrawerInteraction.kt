package com.example.wlauncher.ui.drawer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.foundation.gestures.awaitFirstDown

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
    while (true) {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = down.uptimeMillis
        var dragArmed = false
        var menuShown = false
        var dragStarted = false
        var stillPressed = true
        var totalMovement = Offset.Zero

        while (stillPressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
            val elapsed = change.uptimeMillis - downTime

            if (!dragArmed && elapsed >= DRAWER_DRAG_ARM_MS) {
                dragArmed = true
            }
            if (!menuShown && !dragStarted && elapsed >= DRAWER_MENU_TRIGGER_MS && totalMovement.getDistance() < touchSlop) {
                menuShown = true
                onShowMenu()
            }
            if (menuShown && !dragStarted && elapsed >= DRAWER_MENU_TO_DRAG_MS) {
                onMenuToDrag()
                dragStarted = true
            }

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

        if (dragStarted) {
            onFinishDrag()
        }
    }
}
