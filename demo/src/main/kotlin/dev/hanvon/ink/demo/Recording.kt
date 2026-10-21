package dev.hanvon.ink.demo

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import dev.hanvon.ink.CapabilityReport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** All disk IO is serial; export is a barrier, so the ZIP includes every accepted event before it. */
class Recording(context: Context, capability: CapabilityReport) {
    private val worker = Executors.newSingleThreadExecutor()
    private val queued = AtomicInteger()
    val dropped = AtomicLong()
    val count = AtomicLong()
    val motionCount = AtomicLong()
    val keyCount = AtomicLong()
    val packetCount = AtomicLong()
    val directory = File(context.filesDir, "recordings/${System.currentTimeMillis()}").apply { mkdirs() }
    private val events = File(directory, "events.jsonl")
    private val writer = events.bufferedWriter()
    private val metadata = JSONObject().apply {
        put("schemaVersion", 1); put("createdEpochMillis", System.currentTimeMillis())
        put("uptimeMillis", SystemClock.uptimeMillis()); put("elapsedRealtimeNanos", SystemClock.elapsedRealtimeNanos())
        put("uid", Process.myUid()); put("pid", Process.myPid()); put("model", Build.MODEL)
        put("fingerprint", Build.FINGERPRINT); put("sdkInt", Build.VERSION.SDK_INT)
        put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        put("targetSdk", context.applicationInfo.targetSdkVersion)
        put("available", capability.available); put("origin", capability.origin)
        put("methods", JSONObject(capability.methods)); put("failure", capability.reason)
        put("validation", "unverified; see physical test observations")
        put("timing", "Android eventTime uses uptime; arrival uses elapsedRealtimeNanos. Neither measures visible ink.")
        put("displayModeAtStart", readDisplayMode(context))
    }
    @Volatile private var closed = false
    @Volatile var ioFailure: String? = null; private set
    init { File(directory, "metadata.json").writeText(metadata.toString(2)) }
    fun event(type: String, payload: JSONObject = JSONObject()) {
        if (closed) return
        if (queued.incrementAndGet() > 10_000) { queued.decrementAndGet(); dropped.incrementAndGet(); return }
        val line = JSONObject().put("type", type).put("arrivalNanos", SystemClock.elapsedRealtimeNanos())
            .put("uptimeMillis", SystemClock.uptimeMillis()).put("data", payload).toString()
        worker.execute {
            try { writer.appendLine(line); count.incrementAndGet() }
            catch (e: Exception) { ioFailure = e.toString(); dropped.incrementAndGet() }
            finally { queued.decrementAndGet() }
        }
    }
    fun motion(channel: String, e: MotionEvent) {
        motionCount.incrementAndGet()
        fun sample(history: Int): JSONObject {
            val pointers = JSONArray()
            for (i in 0 until e.pointerCount) pointers.put(JSONObject().apply {
                put("id", e.getPointerId(i)); put("tool", e.getToolType(i))
                put("x", if (history < 0) e.getX(i) else e.getHistoricalX(i, history))
                put("y", if (history < 0) e.getY(i) else e.getHistoricalY(i, history))
                put("pressure", if (history < 0) e.getPressure(i) else e.getHistoricalPressure(i, history))
                put("tilt", if (history < 0) e.getAxisValue(MotionEvent.AXIS_TILT, i) else e.getHistoricalAxisValue(MotionEvent.AXIS_TILT, i, history))
            })
            return JSONObject().put("eventTimeMillis", if (history < 0) e.eventTime else e.getHistoricalEventTime(history))
                .put("pointers", pointers)
        }
        val samples = JSONArray()
        for (i in 0 until e.historySize) samples.put(sample(i))
        samples.put(sample(-1))
        event("motion", JSONObject().put("channel", channel).put("action", e.actionMasked)
            .put("actionIndex", e.actionIndex).put("buttons", e.buttonState).put("actionButton", e.actionButton)
            .put("deviceId", e.deviceId).put("device", e.device?.name).put("source", e.source)
            .put("rawX", e.rawX).put("rawY", e.rawY).put("samples", samples))
    }
    fun key(e: KeyEvent) {
        keyCount.incrementAndGet()
        event("key", JSONObject().put("code", e.keyCode).put("scanCode", e.scanCode)
            .put("action", e.action).put("repeat", e.repeatCount).put("deviceId", e.deviceId)
            .put("source", e.source).put("eventTimeMillis", e.eventTime))
    }
    fun flush() { if (!closed) worker.execute { runCatching { writer.flush() }.onFailure { ioFailure = it.toString() } } }
    fun export(output: java.io.OutputStream, callback: (String?) -> Unit) {
        worker.execute {
            try {
                writer.flush()
                metadata.put("events", count.get()).put("dropped", dropped.get()).put("ioFailure", ioFailure)
                File(directory, "metadata.json").writeText(metadata.toString(2))
                ZipOutputStream(output).use { zip ->
                    listOf("metadata.json", "events.jsonl").forEach { name ->
                        zip.putNextEntry(ZipEntry(name)); File(directory, name).inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                    }
                }
                callback(null)
            } catch (e: Exception) { runCatching { output.close() }; callback(e.toString()) }
        }
    }
    fun close() {
        if (closed) return
        event("recordingClosed", JSONObject().put("dropped", dropped.get()))
        closed = true
        worker.execute { runCatching { writer.close() } }; worker.shutdown()
    }
    companion object {
        fun readDisplayMode(context: Context): String = try {
            val manager = context.getSystemService("eink") ?: return "unavailable"
            manager.javaClass.getMethod("getMode").invoke(manager).toString()
        } catch (e: Exception) { "unavailable: ${e.javaClass.simpleName}" }
    }
}
