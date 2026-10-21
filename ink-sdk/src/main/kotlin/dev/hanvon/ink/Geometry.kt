package dev.hanvon.ink

/** Pixels, never dp. Rectangles describe edges, so transforms use W/H rather than W-1/H-1. */
data class InkPoint(val x: Float, val y: Float)
data class InkRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() })
        require(right >= left && bottom >= top)
    }
    val empty get() = right <= left || bottom <= top
}

/** Recovered screen-to-digitizer conventions; no model-name checks or panel-size constants. */
class CoordinateMap(val origin: Int, val rotation: Int, val width: Float, val height: Float) {
    init { require(origin in 0..2 && rotation in 0..3 && width.isFinite() && height.isFinite() && width > 0 && height > 0) }
    private val turn = (rotation + when (origin) { 0 -> 3; 1 -> 1; else -> 0 }) % 4
    val orientation: Int get() = when (origin) { 0 -> 11; 1 -> 7; else -> 3 } + rotation
    fun toNative(p: InkPoint): InkPoint = when (turn) {
        0 -> p
        1 -> InkPoint(height - p.y, p.x)
        2 -> InkPoint(width - p.x, height - p.y)
        else -> InkPoint(p.y, width - p.x)
    }
    fun toScreen(p: InkPoint): InkPoint = when (turn) {
        0 -> p
        1 -> InkPoint(p.y, height - p.x)
        2 -> InkPoint(width - p.x, height - p.y)
        else -> InkPoint(width - p.y, p.x)
    }
    fun toNative(r: InkRect): InkRect {
        val points = listOf(InkPoint(r.left, r.top), InkPoint(r.right, r.top),
            InkPoint(r.left, r.bottom), InkPoint(r.right, r.bottom)).map(::toNative)
        return InkRect(points.minOf { it.x }, points.minOf { it.y },
            points.maxOf { it.x }, points.maxOf { it.y })
    }
}
