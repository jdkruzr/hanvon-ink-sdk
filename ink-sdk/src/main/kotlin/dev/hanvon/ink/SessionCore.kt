package dev.hanvon.ink

enum class DrawingMode(internal val wireValue: Int) { NATIVE_PREVIEW(3), NATIVE_CALLBACK(0) }
enum class SessionState { INITIALIZING, READY, DRAWING, SUSPENDED, FAILED, CLOSED }
data class InkConfig(val mode: DrawingMode = DrawingMode.NATIVE_CALLBACK, val widthPx: Int = 4,
    val settlingMillis: Long = 1000) {
    init { require(widthPx in 1..64 && settlingMillis in 0..5000) }
}

/** Called only on the shared serial service worker. Every owned handle is released at most once. */
internal class SessionCore(private val transport: PenTransport, private val config: InkConfig) {
    private var handle: Long? = null
    private var closed = false
    fun open(callback: (Boolean, Long, FloatArray) -> Unit) {
        check(!closed && handle == null)
        val created = transport.init(callback)
        check(created != 0L) { "initService returned zero; no usable area handle" }
        handle = created
        transport.call("setAreaActive", created, false)
        transport.call("enablePen", created, false)
        transport.call("setPen", created, 15)
        transport.call("setPenColor", created, 1)
        transport.call("setPenWidth", created, config.widthPx)
        transport.call("setDrawStatus", created, config.mode.wireValue)
    }
    private fun owned(): Long = checkNotNull(handle) { "Session not initialized" }
    fun configure(map: CoordinateMap, area: InkRect, exclusion: InkRect?) {
        val h = owned()
        transport.call("enablePen", h, false)
        transport.call("setAreaActive", h, false)
        transport.call("setOrientation", map.orientation)
        rectCall("setDrawArea", map.toNative(area))
        rectCall("setExceptionArea", exclusion?.let(map::toNative) ?: InkRect(0f, 0f, 0f, 0f))
    }
    private fun rectCall(name: String, rect: InkRect) {
        transport.call(name, owned(), kotlin.math.floor(rect.left).toInt(), kotlin.math.floor(rect.top).toInt(),
            kotlin.math.ceil(rect.right).toInt(), kotlin.math.ceil(rect.bottom).toInt())
    }
    fun activate() {
        transport.call("setAreaActive", owned(), true)
        transport.call("enablePen", owned(), config.mode == DrawingMode.NATIVE_CALLBACK)
    }
    fun suspend() {
        val h = handle ?: return
        transport.call("enablePen", h, false)
        transport.call("setAreaActive", h, false)
    }
    fun pen(down: Boolean) { transport.call("enablePen", owned(), down) }
    fun width(width: Int) { transport.call("setPenWidth", owned(), width) }
    fun clearPreview() { transport.call("resetData") }
    fun close(): List<Throwable> {
        if (closed) return emptyList()
        closed = true
        val h = handle ?: return emptyList()
        handle = null
        val failures = mutableListOf<Throwable>()
        // Continue teardown after a failed setter. Never reset global data during teardown.
        listOf<() -> Unit>({ transport.call("enablePen", h, false) },
            { transport.call("setAreaActive", h, false) }, { transport.end(h) }).forEach {
            try { it() } catch (e: Throwable) { failures += e }
        }
        return failures
    }
}

/** Prevent a late frame or timeout from erasing a newer native stroke. */
internal class HandoffGate {
    var sequence = 0L; private set
    private var down = false
    private var endedAt: Long? = null
    private var committed = false
    @Synchronized fun begin(): Long {
        sequence++; down = true; endedAt = null; committed = false
        return sequence
    }
    @Synchronized fun end(now: Long): Long { down = false; endedAt = now; return sequence }
    @Synchronized fun frame(token: Long) { if (token == sequence && endedAt != null) committed = true }
    @Synchronized fun invalidate() { sequence++; down = false; endedAt = null; committed = false }
    @Synchronized fun delay(token: Long, now: Long, settling: Long): Long? {
        val end = endedAt ?: return null
        if (token != sequence || down || !committed) return null
        return (end + settling - now).coerceAtLeast(0)
    }
    @Synchronized fun cleared(token: Long) { if (token == sequence) { endedAt = null; committed = false } }
}
