package dev.hanvon.ink

import android.graphics.Canvas
import android.os.Build
import android.view.View

/** Software submission evidence, never a claim that the physical e-ink panel has updated. */
object RetainedFrame {
    enum class Signal { FRAME_COMMITTED, POST_DRAW_APPROXIMATION }

    /**
     * Call once from onDraw after painting the retained content. Keep the stroke/session/revision
     * captured by [committed] and reject stale callbacks in the host. Hardware callback collection
     * may precede onDraw, so explicitly schedule a following frame rather than waiting for user input.
     */
    fun afterDraw(view: View, canvas: Canvas, committed: (Signal) -> Unit) {
        if (Build.VERSION.SDK_INT >= 29 && canvas.isHardwareAccelerated) {
            view.viewTreeObserver.registerFrameCommitCallback {
                view.post { committed(Signal.FRAME_COMMITTED) }
            }
            view.postInvalidateOnAnimation()
        } else view.post { committed(Signal.POST_DRAW_APPROXIMATION) }
    }
}
