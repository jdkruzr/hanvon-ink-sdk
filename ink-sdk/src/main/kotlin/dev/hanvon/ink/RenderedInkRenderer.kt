package dev.hanvon.ink

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Optional stateless geometry contract, with a reusable Paint; use each instance on one thread.
 * Matches the stock plain pressure pen's circles at the supplied native samples. */
class RenderedInkRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isDither = true }
    fun draw(canvas: Canvas, stroke: RenderedStroke, color: Int = Color.BLACK,
             scale: Float = 1f, offsetX: Float = 0f, offsetY: Float = 0f) {
        require(scale.isFinite() && scale > 0)
        paint.color = color
        stroke.points.forEach { canvas.drawCircle(it.x * scale + offsetX, it.y * scale + offsetY, it.width * scale / 2, paint) }
    }
}
