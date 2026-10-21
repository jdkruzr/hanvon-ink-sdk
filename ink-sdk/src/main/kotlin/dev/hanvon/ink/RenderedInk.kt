package dev.hanvon.ink

import java.util.Collections

/** Recovered style identifiers; availability is not evidence of visual fidelity on a device. */
enum class NativeBrush(internal val wireValue: Int) { PENCIL(15), PRESSURE_PEN(6) }

/** Monochrome firmware preview codes, not ARGB colors or retained-ink opacity.
 * Stock Notes uses PENCIL plus DARK_GRAY/LIGHT_GRAY for its marker preview.
 * Hosts must replace that preview with their retained marker after frame submission.
 */
enum class NativePreviewColor(internal val wireValue: Int) {
    BLACK(1), WHITE(2), DARK_GRAY(3), LIGHT_GRAY(4)
}

/** Width is the diameter actually reported by firmware, in the same units as x/y. Not pressure. */
data class RenderedPoint(val x: Float, val y: Float, val width: Float) {
    init { require(x.isFinite() && y.isFinite() && width.isFinite() && width >= 0) }
}

/** Defensive immutable copy. No fabricated pressure or point timestamps. */
class RenderedStroke(points: List<RenderedPoint>) {
    val points: List<RenderedPoint> = Collections.unmodifiableList(ArrayList(points))
    init { require(points.isNotEmpty() && points.size <= 80_000) }
    fun transformed(scale: Float, offsetX: Float = 0f, offsetY: Float = 0f): RenderedStroke {
        require(scale.isFinite() && scale > 0)
        return RenderedStroke(points.map { RenderedPoint(it.x * scale + offsetX, it.y * scale + offsetY, it.width * scale) })
    }
}
