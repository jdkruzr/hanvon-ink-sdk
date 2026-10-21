package dev.hanvon.ink

import android.os.SystemClock
import android.view.MotionEvent

object InputClock {
    /** Android input uses uptime; native callback receipt uses elapsed realtime. Convert the
     * event timestamp, not its possibly delayed UI dispatch time. Recompute across sleep/wake. */
    fun eventNanos(event: MotionEvent): Long = SystemClock.elapsedRealtimeNanos() -
        (SystemClock.uptimeMillis() - event.eventTime).coerceAtLeast(0) * 1_000_000L
}
