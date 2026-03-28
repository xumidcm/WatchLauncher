package com.example.wlauncher.ui.drawer

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import kotlin.math.abs
import kotlin.math.sign

internal const val DRAWER_DRAG_ARM_MS = 500L
internal const val DRAWER_MENU_TRIGGER_MS = 1000L
internal const val DRAWER_MENU_TO_DRAG_MS = 1500L

internal enum class DrawerGesturePhase {
    Idle,
    Pressing,
    MenuOpen,
    Dragging,
    Settling
}

internal data class DrawerPreviewOrderState(
    val baseKeys: List<String>,
    val draggingKey: String? = null,
    val dragFromIndex: Int = -1,
    val dragTargetIndex: Int = -1
) {
    val isDragging: Boolean
        get() = draggingKey != null && dragFromIndex >= 0 && dragTargetIndex >= 0

    val settledKeys: List<String>
        get() = if (!isDragging) baseKeys else moveKey(baseKeys, dragFromIndex, dragTargetIndex)

    fun beginDrag(key: String, index: Int): DrawerPreviewOrderState {
        return copy(
            draggingKey = key,
            dragFromIndex = index,
            dragTargetIndex = index
        )
    }

    fun updateTarget(index: Int): DrawerPreviewOrderState {
        if (!isDragging || index == dragTargetIndex) return this
        return copy(dragTargetIndex = index)
    }

    fun clearDrag(nextKeys: List<String> = settledKeys): DrawerPreviewOrderState {
        return DrawerPreviewOrderState(baseKeys = nextKeys)
    }
}

internal data class DrawerAutoScrollSpec(
    val viewportHeight: Float,
    val thresholdPx: Float,
    val maxVelocityPxPerSecond: Float,
    val accelerationPxPerSecond2: Float,
    val decelerationPxPerSecond2: Float
)

internal fun moveKey(keys: List<String>, fromIndex: Int, toIndex: Int): List<String> {
    if (fromIndex !in keys.indices || toIndex !in keys.indices || fromIndex == toIndex) return keys
    val mutable = keys.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}

internal fun targetAutoScrollVelocity(pointerY: Float, spec: DrawerAutoScrollSpec): Float {
    if (spec.thresholdPx <= 0f || spec.maxVelocityPxPerSecond <= 0f) return 0f
    return when {
        pointerY < spec.thresholdPx -> {
            val progress = (1f - pointerY / spec.thresholdPx).coerceIn(0f, 1f)
            -spec.maxVelocityPxPerSecond * progress * progress * progress
        }

        pointerY > spec.viewportHeight - spec.thresholdPx -> {
            val progress = (1f - (spec.viewportHeight - pointerY) / spec.thresholdPx).coerceIn(0f, 1f)
            spec.maxVelocityPxPerSecond * progress * progress * progress
        }

        else -> 0f
    }
}

internal fun stepAutoScrollVelocity(
    current: Float,
    target: Float,
    deltaSeconds: Float,
    spec: DrawerAutoScrollSpec
): Float {
    if (deltaSeconds <= 0f) return current
    if (abs(target - current) < 1f) return target
    val sameDirection = current == 0f || sign(current) == sign(target)
    val rate = if (sameDirection && abs(target) > abs(current)) {
        spec.accelerationPxPerSecond2
    } else {
        spec.decelerationPxPerSecond2
    }
    val step = rate * deltaSeconds
    return when {
        current < target -> (current + step).coerceAtMost(target)
        current > target -> (current - step).coerceAtLeast(target)
        else -> current
    }
}

internal suspend fun AwaitPointerEventScope.runDrawerLongPressSequence(
    touchSlop: Float,
    onShowMenu: () -> Unit,
    onMenuToDrag: (Offset) -> Unit,
    onBeginDrag: (Offset) -> Unit,
    onDragDelta: (Offset, Offset) -> Unit,
    onFinishDrag: () -> Unit,
    onTap: (Offset) -> Unit = {},
    onPressStateChange: (Boolean) -> Unit = {},
    onPhaseChange: (DrawerGesturePhase) -> Unit = {}
) {
    while (true) {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = down.uptimeMillis
        var dragArmed = false
        var menuShown = false
        var dragStarted = false
        var stillPressed = true
        var totalMovement = Offset.Zero
        var lastPosition = down.position

        onPressStateChange(true)
        onPhaseChange(DrawerGesturePhase.Pressing)

        while (stillPressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
            val elapsed = change.uptimeMillis - downTime
            lastPosition = change.position

            if (!dragArmed && elapsed >= DRAWER_DRAG_ARM_MS) {
                dragArmed = true
            }
            if (!menuShown && !dragStarted && elapsed >= DRAWER_MENU_TRIGGER_MS && totalMovement.getDistance() < touchSlop) {
                menuShown = true
                onPhaseChange(DrawerGesturePhase.MenuOpen)
                onShowMenu()
            }
            if (menuShown && !dragStarted && elapsed >= DRAWER_MENU_TO_DRAG_MS) {
                dragStarted = true
                onPhaseChange(DrawerGesturePhase.Dragging)
                onMenuToDrag(lastPosition)
            }

            if (!change.pressed) {
                stillPressed = false
                break
            }

            val delta = change.position - change.previousPosition
            totalMovement += delta
            if (!menuShown && !dragStarted && dragArmed && totalMovement.getDistance() > touchSlop) {
                dragStarted = true
                onPhaseChange(DrawerGesturePhase.Dragging)
                onBeginDrag(lastPosition)
            }

            if (dragStarted) {
                change.consume()
                onDragDelta(delta, change.position)
            }
        }

        onPressStateChange(false)

        if (dragStarted) {
            onPhaseChange(DrawerGesturePhase.Settling)
            onFinishDrag()
        } else if (!menuShown && totalMovement.getDistance() < touchSlop) {
            onTap(lastPosition)
        }

        onPhaseChange(DrawerGesturePhase.Idle)
    }
}
