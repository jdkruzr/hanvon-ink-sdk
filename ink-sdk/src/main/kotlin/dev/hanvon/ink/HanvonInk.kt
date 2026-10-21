package dev.hanvon.ink

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import java.util.concurrent.Executors
import java.lang.ref.WeakReference

interface InkListener {
    fun onState(state: SessionState, detail: String) {}
    fun onNativePacket(packet: NativePacket) {}
    /** View-local pixels; packet semantics, not a guaranteed unique stroke. */
    fun onRenderedPacket(stroke: RenderedStroke, arrivalNanos: Long) {}
    /** Coordinate geometry changed; discard any host capture tied to the previous mapping. */
    fun onGeometryChanged() {}
    fun onDiagnostic(operation: String, detail: String, elapsedNanos: Long) {}
}

object HanvonInk {
    internal val worker = Executors.newSingleThreadExecutor { Thread(it, "hanvon-ink-service") }
    private var owner = WeakReference<InkSession>(null)
    fun probe(context: Context): CapabilityReport = ReflectionTransport.discover(context).first
    /** Call on main thread. A closing session finishes teardown before the next session opens. */
    fun attach(view: View, config: InkConfig = InkConfig(), listener: InkListener = object : InkListener {}): InkSession {
        check(Looper.myLooper() == Looper.getMainLooper()) { "attach requires main thread" }
        check(owner.get() == null) { "Close the existing ink session before attaching another" }
        val (report, transport) = ReflectionTransport.discover(view.context)
        check(report.available && transport != null) { report.reason ?: "Native interface unavailable" }
        return InkSession(view, config, report, transport, listener).also { owner = WeakReference(it); it.start() }
    }
    internal fun release(session: InkSession) { if (owner.get() === session) owner.clear() }
}

class InkSession internal constructor(private val view: View, val config: InkConfig,
    val capability: CapabilityReport, transport: PenTransport, private val listener: InkListener) : AutoCloseable {
    private val ui = Handler(Looper.getMainLooper())
    private val core = SessionCore(transport, config)
    private val handoff = HandoffGate()
    @Volatile private var closed = false
    @Volatile private var failed = false
    @Volatile private var foreground = false
    @Volatile private var epoch = 0L
    private var hostResumed = false
    @Volatile private var blocked = false
    @Volatile private var redrawAllowed = false
    private var localArea: InkRect? = null
    private var exclusion: InkRect? = null
    private var geometry: String? = null
    private var coordinateGeometry: String? = null
    private val displays = view.context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) { update() }
        override fun onDisplayChanged(displayId: Int) {
            if (view.display?.displayId == displayId) { update(); view.invalidate() }
        }
    }
    private var retainingStroke = false
    var state = SessionState.INITIALIZING; private set
    var coordinateMap: CoordinateMap? = null; private set
    private val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { update() }
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { update() }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { update() }
        override fun onViewDetachedFromWindow(v: View) { close() }
    }
    private fun mainThread() { check(Looper.myLooper() == Looper.getMainLooper()) }
    private fun diagnostic(op: String, detail: String, duration: Long = 0) {
        ui.post { listener.onDiagnostic(op, detail, duration) }
    }
    private fun state(next: SessionState, detail: String) {
        ui.post {
            if ((!closed || next == SessionState.CLOSED) && (!failed || next in setOf(SessionState.FAILED, SessionState.CLOSED))) {
                state = next; listener.onState(next, detail)
            }
        }
    }
    private fun task(op: String, guard: () -> Boolean = { !closed && !failed }, action: () -> Unit) {
        HanvonInk.worker.execute {
            if (!guard()) return@execute
            val started = SystemClock.elapsedRealtimeNanos()
            try {
                action()
                diagnostic(op, "returned; physical effect unverified", SystemClock.elapsedRealtimeNanos() - started)
            } catch (e: Throwable) {
                failed = true; foreground = false; epoch++; handoff.invalidate()
                val cleanup = core.close()
                diagnostic(op, "${e.javaClass.simpleName}: ${e.message}; cleanup failures=${cleanup.map { it.toString() }}")
                state(SessionState.FAILED, "$op: ${e.message}")
            }
        }
    }
    internal fun start() {
        displays?.registerDisplayListener(displayListener, ui)
        view.addOnLayoutChangeListener(layoutListener)
        view.addOnAttachStateChangeListener(attachListener)
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        view.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        state(SessionState.INITIALIZING, "Opening native area")
        task("initialize") {
            core.open { flag, arrival, values ->
                if (!closed && !failed) {
                    try {
                        val callbackEpoch = epoch
                        val packet = NativePacket.parse(flag, arrival, values)
                        ui.post { if (!closed && !failed && foreground && epoch == callbackEpoch) {
                            listener.onNativePacket(packet)
                            val map = coordinateMap
                            if (!closed && !failed && foreground && epoch == callbackEpoch && map != null && packet.points.isNotEmpty()) {
                                val pos = IntArray(2); view.getLocationOnScreen(pos)
                                val stroke = RenderedStroke(packet.points.map {
                                    val p = map.toScreen(InkPoint(it.x, it.y))
                                    RenderedPoint(p.x - pos[0], p.y - pos[1], it.renderedWidth)
                                })
                                listener.onRenderedPacket(stroke, packet.arrivalNanos)
                            }
                        } }
                    } catch (e: IllegalArgumentException) { diagnostic("packetRejected", e.message ?: "invalid packet") }
                }
            }
            state(SessionState.SUSPENDED, "Area initialized; waiting for foreground geometry")
        }
        update()
    }
    /** Host must pass resumed && !multiwindow; SDK also watches window focus and attachment. */
    fun setForeground(resumed: Boolean) { mainThread(); hostResumed = resumed; update() }
    fun suspendDrawing() { mainThread(); blocked = true; update() }
    fun resumeDrawing() { mainThread(); blocked = false; update() }
    fun setBounds(areaInView: InkRect? = null, excludedInView: InkRect? = null) {
        mainThread(); localArea = areaInView; exclusion = excludedInView; geometry = null; update()
    }
    /** Configure while suspended; callers resume only when their matching retained renderer is ready. */
    fun setBrush(brush: NativeBrush) { mainThread(); task("brush") { core.brush(brush) } }
    fun setWidth(widthPx: Int) {
        mainThread(); require(widthPx in 1..64)
        task("width") { core.width(widthPx) }
    }
    @Suppress("DEPRECATION")
    private fun update() {
        mainThread()
        if (closed || failed) return
        val canRedraw = hostResumed && view.isAttachedToWindow && view.hasWindowFocus() && view.isShown
        if (redrawAllowed != canRedraw) { redrawAllowed = canRedraw; epoch++ }
        val active = hostResumed && !blocked && view.isAttachedToWindow && view.hasWindowFocus() &&
            view.isShown && view.width > 0 && view.height > 0
        val visible = Rect()
        if (!active || !view.getLocalVisibleRect(visible)) {
            if (foreground) {
                foreground = false; epoch++; handoff.invalidate(); geometry = null
                task("suspend") { core.suspend(); state(SessionState.SUSPENDED, "Surface not active") }
            }
            return
        }
        val display = view.display ?: return
        val size = Point(); display.getRealSize(size)
        val map = CoordinateMap(capability.origin!!, display.rotation, size.x.toFloat(), size.y.toFloat())
        val pos = IntArray(2); view.getLocationOnScreen(pos)
        val coordinateKey = "${display.rotation}:${size.x}:${size.y}:${pos[0]}:${pos[1]}:${view.width}:${view.height}"
        if (coordinateGeometry != coordinateKey) {
            val changed = coordinateGeometry != null
            coordinateGeometry = coordinateKey
            if (changed) ui.post {
                if (!closed && !failed && coordinateGeometry == coordinateKey) listener.onGeometryChanged()
            }
        }
        visible.offset(pos[0], pos[1])
        val bounds = localArea ?: InkRect(0f, 0f, view.width.toFloat(), view.height.toFloat())
        require(bounds.left >= 0 && bounds.top >= 0 && bounds.right <= view.width && bounds.bottom <= view.height)
        fun screen(r: InkRect) = InkRect(r.left + pos[0], r.top + pos[1], r.right + pos[0], r.bottom + pos[1])
        val requested = screen(bounds)
        val screenBounds = InkRect(
            requested.left.coerceIn(visible.left.toFloat(), visible.right.toFloat()),
            requested.top.coerceIn(visible.top.toFloat(), visible.bottom.toFloat()),
            requested.right.coerceIn(visible.left.toFloat(), visible.right.toFloat()),
            requested.bottom.coerceIn(visible.top.toFloat(), visible.bottom.toFloat()))
        val excluded = exclusion?.also {
            require(it.left >= 0 && it.top >= 0 && it.right <= view.width && it.bottom <= view.height)
        }?.let(::screen)
        val key = "${display.rotation}:${size.x}:${size.y}:$screenBounds:$excluded"
        if (foreground && geometry == key) return
        geometry = key; coordinateMap = map
        epoch++; val revision = epoch; handoff.invalidate(); foreground = !screenBounds.empty
        task("geometry", { !closed && !failed && epoch == revision }) {
            core.configure(map, screenBounds, excluded)
            if (foreground && epoch == revision) {
                core.activate()
                state(SessionState.READY, "Native area configured; visible drawing unverified")
            }
        }
    }
    /** Observe, do not consume. Return token for notifyFrameCommitted after UP/CANCEL. */
    fun observe(event: MotionEvent, retainStroke: Boolean = true): Long {
        mainThread()
        if (!foreground || closed || failed) return -1
        val revision = epoch
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                retainingStroke = retainStroke
                if (!retainingStroke) {
                    handoff.invalidate()
                    if (config.mode == DrawingMode.NATIVE_PREVIEW) task("unretainedInput") { core.pen(false) }
                    return -1
                }
                handoff.begin()
                if (config.mode == DrawingMode.NATIVE_PREVIEW) task("penDown", { foreground && !closed && !failed && epoch == revision }) { core.pen(true) }
                state(SessionState.DRAWING, "Android stroke observed")
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!retainingStroke) return -1
                retainingStroke = false
                handoff.end(SystemClock.uptimeMillis())
                if (config.mode == DrawingMode.NATIVE_PREVIEW) task("penUp", { !closed && !failed && epoch == revision }) { core.pen(false) }
                state(SessionState.READY, "Waiting for retained frame")
            }
        }
        return handoff.sequence
    }
    fun notifyFrameCommitted(token: Long) {
        mainThread()
        if (closed || failed || !foreground || token < 0) return
        handoff.frame(token)
        val delay = handoff.delay(token, SystemClock.uptimeMillis(), config.settlingMillis)
        diagnostic("retainedFrame", "token=$token current=${handoff.sequence} delay=$delay")
        if (delay == null) return
        ui.postDelayed({
            if (!foreground || closed || failed) return@postDelayed
            task("clearPreview", {
                foreground && !closed && !failed && handoff.delay(token, SystemClock.uptimeMillis(), config.settlingMillis) == 0L
            }) { core.clearPreview(); handoff.cleared(token) }
        }, delay)
    }
    /**
     * Clear this foreground owner's old preview after an edited retained frame has been submitted.
     * Suspend drawing before editing and keep it suspended until completion. Unlike a stroke handoff,
     * this also covers erase, selection and viewport changes, which have no native stroke token.
     * A late frame after resume, focus loss or close never resets the global firmware buffer.
     * [completed] receives false if ownership changed before the reset could run.
     */
    fun notifyRetainedRedrawCommitted(completed: (Boolean) -> Unit) {
        mainThread()
        val revision = epoch
        fun eligible() = !closed && !failed && blocked && hostResumed &&
            view.isAttachedToWindow && view.hasWindowFocus() && view.isShown
        if (!eligible()) { completed(false); return }
        HanvonInk.worker.execute {
            var cleared = false
            try {
                // View properties are checked on the main thread; epoch/blocked guard worker ownership.
                if (!closed && !failed && blocked && redrawAllowed && epoch == revision) {
                    core.clearPreview(); cleared = true
                    diagnostic("clearEditedPreview", "retained redraw submitted")
                }
            } catch (e: Throwable) {
                failed = true; foreground = false; epoch++; handoff.invalidate()
                val cleanup = core.close()
                diagnostic("clearEditedPreview", "${e.javaClass.simpleName}: ${e.message}; cleanup failures=$cleanup")
                state(SessionState.FAILED, "Edited preview cleanup failed: ${e.message}")
            }
            ui.post { completed(cleared && eligible() && epoch == revision) }
        }
    }
    override fun close() {
        mainThread()
        if (closed) return
        closed = true; foreground = false; epoch++; handoff.invalidate()
        ui.removeCallbacksAndMessages(null)
        displays?.unregisterDisplayListener(displayListener)
        view.removeOnLayoutChangeListener(layoutListener)
        view.removeOnAttachStateChangeListener(attachListener)
        if (view.viewTreeObserver.isAlive) {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            view.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        }
        HanvonInk.worker.execute {
            val errors = core.close()
            diagnostic("close", if (errors.isEmpty()) "Owned area released" else errors.joinToString { it.toString() })
            state(SessionState.CLOSED, "Closed")
        }
        HanvonInk.release(this)
    }
}
