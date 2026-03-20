package com.example.wlauncher.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 六边形网格轴坐标。
 * q = 列, r = 行 (axial coordinate system)
 */
data class HexCoord(val q: Int, val r: Int)

/**
 * 生成从中心向外的螺旋排列六边形坐标。
 * @param count 需要的坐标数量
 * @return 按螺旋顺序排列的坐标列表
 */
fun generateHexSpiral(count: Int): List<HexCoord> {
    if (count <= 0) return emptyList()
    val result = mutableListOf(HexCoord(0, 0))
    if (count == 1) return result

    // 六方向: E, NE, NW, W, SW, SE
    val directions = listOf(
        HexCoord(1, 0), HexCoord(0, -1), HexCoord(-1, -1),
        HexCoord(-1, 0), HexCoord(0, 1), HexCoord(1, 1)
    )

    var ring = 1
    while (result.size < count) {
        var q = ring
        var r = 0
        for (dir in 0..5) {
            for (step in 0 until ring) {
                if (result.size >= count) return result
                result.add(HexCoord(q, r))
                q += directions[dir].q
                r += directions[dir].r
            }
        }
        ring++
    }
    return result
}

/**
 * 轴坐标 → 像素坐标 (pointy-top hex)
 * @param hex 六边形坐标
 * @param size 六边形边长(像素)
 * @return 相对于中心(0,0)的像素偏移
 */
fun hexToPixel(hex: HexCoord, size: Float): Offset {
    val x = size * (sqrt(3f) * hex.q + sqrt(3f) / 2f * hex.r)
    val y = size * (3f / 2f * hex.r)
    return Offset(x, y)
}

/**
 * 鱼眼缩放：离中心越近的图标越大。
 * @param distance 图标中心到屏幕中心的距离(像素)
 * @param maxDistance 最大距离参考值(通常为屏幕半径)
 * @param maxScale 中心处的缩放值
 * @param minScale 边缘处的缩放值
 */
fun fisheyeScale(
    distance: Float,
    maxDistance: Float,
    maxScale: Float = 1f,
    minScale: Float = 0.45f
): Float {
    val t = (distance / maxDistance).coerceIn(0f, 1f)
    return maxScale - (maxScale - minScale) * t
}
