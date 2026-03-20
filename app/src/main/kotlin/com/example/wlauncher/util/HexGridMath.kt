package com.example.wlauncher.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

data class HexCoord(val q: Int, val r: Int)

fun generateHexSpiral(count: Int): List<HexCoord> {
    if (count <= 0) return emptyList()

    val result = mutableListOf(HexCoord(0, 0))
    if (count == 1) return result

    val directions = listOf(
        HexCoord(1, 0),
        HexCoord(1, -1),
        HexCoord(0, -1),
        HexCoord(-1, 0),
        HexCoord(-1, 1),
        HexCoord(0, 1)
    )

    var ring = 1
    while (result.size < count) {
        var current = HexCoord(-ring, ring)
        for (dir in directions) {
            repeat(ring) {
                if (result.size >= count) return result
                result.add(current)
                current = HexCoord(current.q + dir.q, current.r + dir.r)
            }
        }
        ring++
    }

    return result
}

fun hexToPixel(hex: HexCoord, size: Float): Offset {
    val x = size * (sqrt(3f) * hex.q + sqrt(3f) / 2f * hex.r)
    val y = size * (3f / 2f * hex.r)
    return Offset(x, y)
}

fun fisheyeScale(
    distance: Float,
    maxDistance: Float,
    maxScale: Float = 1f,
    minScale: Float = 0.45f
): Float {
    val t = (distance / maxDistance).coerceIn(0f, 1f)
    return maxScale - (maxScale - minScale) * t
}
