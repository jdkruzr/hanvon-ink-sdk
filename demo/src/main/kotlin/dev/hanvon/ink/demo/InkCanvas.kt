package dev.hanvon.ink.demo

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import dev.hanvon.ink.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.min

data class Stroke(val width: Float, val points: MutableList<InkPoint>, var nativeInk: RenderedStroke? = null)

class InkCanvas(context: Context, private val recording: Recording) : View(context) {
    var session: InkSession? = null
    var pressureMode = false
    private val nativeRenderer = RenderedInkRenderer()
    private val capture = StrokeCapture()
    private val captured = linkedMapOf<Long, Stroke>()
    private var transaction = 0L
    private var capturing = false
    private val finishCapture = Runnable {
        capture.finish(SystemClock.elapsedRealtimeNanos()).forEach { result ->
            captured.remove(result.id)?.let { stroke ->
                result.stroke?.let { stroke.nativeInk = it.transformed(1f / scale, -offsetX / scale, -offsetY / scale) }
                recording.event("nativeAssociation", JSONObject().put("id", result.id).put("accepted", result.stroke != null).put("reason", result.reason))
            }
        }
        rebuild(); save(); scheduleFrame(pendingToken)
    }
    var widthPx = 4
    var exclusionEnabled = false
        set(value) { field = value; updateExclusion(); invalidate() }
    var drawRetained = true
    private val strokes = mutableListOf<Stroke>()
    private var current: Stroke? = null
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val storeWorker = Executors.newSingleThreadExecutor()
    private val store = File(context.filesDir, "strokes.json")
    private var docW = 0f
    private var docH = 0f
    private var callbackPoints = emptyList<InkPoint>()
    private var pendingFrame = false
    private var pendingToken = -1L
    private var frameSession: InkSession? = null
    var strokeCount: Int = 0; private set
    var onStroke: (() -> Unit)? = null
    private val scale get() = if (docW > 0 && docH > 0) min(width / docW, height / docH) else 1f
    private val offsetX get() = (width - docW * scale) / 2
    private val offsetY get() = (height - docH * scale) / 2
    init {
        setBackgroundColor(Color.WHITE)
        isFocusable = true
        try {
            if (store.exists()) {
                val root = JSONObject(store.readText()); docW = root.getDouble("width").toFloat(); docH = root.getDouble("height").toFloat()
                require(docW.isFinite() && docH.isFinite() && docW > 0 && docH > 0)
                val list = root.getJSONArray("strokes")
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i); val points = item.getJSONArray("points")
                    val stroke = Stroke(item.getDouble("width").toFloat(), mutableListOf())
                    for (j in 0 until points.length()) {
                        val point = points.getJSONArray(j)
                        stroke.points += InkPoint(point.getDouble(0).toFloat(), point.getDouble(1).toFloat())
                    }
                    item.optJSONArray("native")?.let { native ->
                        stroke.nativeInk = RenderedStroke((0 until native.length()).map { j ->
                            val p = native.getJSONArray(j); RenderedPoint(p.getDouble(0).toFloat(), p.getDouble(1).toFloat(), p.getDouble(2).toFloat())
                        })
                    }
                    strokes += stroke
                }
            }
        } catch (e: Exception) { strokes.clear(); recording.event("strokeLoadFailed", JSONObject().put("error", e.toString())) }
        strokeCount = strokes.size
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        cancelStroke()
        if (docW <= 0 || docH <= 0) { docW = w.toFloat(); docH = h.toFloat() }
        bitmap?.recycle(); bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); bitmapCanvas = Canvas(bitmap!!)
        rebuild(); updateExclusion()
    }
    private fun exclusionRect(): InkRect? = if (exclusionEnabled && width > 0 && height > 0)
        InkRect(width * .4f, height * .4f, width * .6f, height * .6f) else null
    fun updateExclusion() { session?.setBounds(excludedInView = exclusionRect()) }
    private fun inExclusion(x: Float, y: Float): Boolean {
        val r = exclusionRect() ?: return false
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    }
    private fun segment(canvas: Canvas, stroke: Stroke, previous: InkPoint?, next: InkPoint) {
        paint.color = Color.BLACK; paint.style = Paint.Style.STROKE; paint.strokeWidth = stroke.width * scale
        val x = next.x * scale + offsetX; val y = next.y * scale + offsetY
        if (previous == null) canvas.drawPoint(x, y, paint)
        else canvas.drawLine(previous.x * scale + offsetX, previous.y * scale + offsetY, x, y, paint)
    }
    private fun rebuild() {
        val canvas = bitmapCanvas ?: return
        canvas.drawColor(Color.WHITE)
        canvas.save()
        exclusionRect()?.let { canvas.clipOutRect(it.left, it.top, it.right, it.bottom) }
        strokes.forEach { stroke ->
            val native = stroke.nativeInk
            if (native != null) nativeRenderer.draw(canvas, native, scale = scale, offsetX = offsetX, offsetY = offsetY)
            else stroke.points.forEachIndexed { i, point -> segment(canvas, stroke, stroke.points.getOrNull(i - 1), point) }
        }
        canvas.restore()
    }
    override fun onDraw(canvas: Canvas) {
        val started = SystemClock.elapsedRealtimeNanos()
        if (drawRetained) bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        paint.color = Color.GRAY; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f
        canvas.drawRect(1f, 1f, width - 1f, height - 1f, paint)
        exclusionRect()?.let {
            canvas.drawRect(it.left, it.top, it.right, it.bottom, paint)
            paint.textSize = 18 * resources.displayMetrics.scaledDensity
            paint.style = Paint.Style.FILL; canvas.drawText("Excluded", it.left + 8, it.top + 28, paint)
        }
        // Inspection markers are deliberately distinct from retained black strokes.
        paint.color = Color.GRAY; paint.style = Paint.Style.STROKE
        callbackPoints.forEach { canvas.drawCircle(it.x, it.y, 3f, paint) }
        if (pendingFrame) {
            pendingFrame = false
            val token = pendingToken; val owner = frameSession
            RetainedFrame.afterDraw(this, canvas) { signal ->
                recording.event("retainedFrame", JSONObject().put("token", token).put("drawToFrameSignalNanos", SystemClock.elapsedRealtimeNanos() - started)
                    .put("signal", signal.name))
                if (session === owner) owner?.notifyFrameCommitted(token)
            }
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        recording.motion("canvasTouch", event)
        val tool = event.getToolType(0)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val signals = PenInput.classify(tool, event.buttonState, event.source)
            recording.event("penSignals", JSONObject().put("tool", signals.tool.name).put("primary", signals.primary)
                .put("secondary", signals.secondary).put("tertiary", signals.tertiary).put("deviceId", event.deviceId)
                .put("source", event.source).put("deviceName", event.device?.name))
        }
        val drawingTool = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_UNKNOWN || tool == 6
        val token = session?.observe(event, drawingTool && !inExclusion(event.x, event.y)) ?: -1
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                current = if (drawingTool && !inExclusion(event.x, event.y)) Stroke(widthPx / scale, mutableListOf()) else null
                capturing = pressureMode && session != null && current != null
                if (capturing) {
                    removeCallbacks(finishCapture); transaction++
                    capturing = capture.begin(transaction, InputClock.eventNanos(event), InkPoint(event.x, event.y))
                    if (capturing) captured[transaction] = current!!
                    else recording.event("captureLimit", JSONObject().put("id", transaction))
                }
                current?.let { add(event.x, event.y) }
                recording.event("strokeStart", JSONObject().put("tool", tool).put("retained", current != null))
            }
            MotionEvent.ACTION_MOVE -> {
                if (current != null) {
                    for (i in 0 until event.historySize) add(event.getHistoricalX(i), event.getHistoricalY(i))
                    add(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP -> {
                current?.let {
                    add(event.x, event.y); strokes += it; current = null; strokeCount = strokes.size
                    save(); onStroke?.invoke()
                }
                if (capturing) {
                    capture.end(transaction, InputClock.eventNanos(event)); pendingToken = token
                    postDelayed(finishCapture, 1050)
                } else {
                    scheduleFrame(token)
                    if (captured.isNotEmpty()) postDelayed(finishCapture, 1050)
                }
                capturing = false
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> { cancelStroke(); scheduleFrame(token) }
        }
        if (!pressureMode || session == null) invalidate()
        return true
    }
    private fun add(x: Float, y: Float) {
        val stroke = current ?: return
        val canvas = bitmapCanvas ?: return
        val point = InkPoint((x - offsetX) / scale, (y - offsetY) / scale)
        canvas.save()
        exclusionRect()?.let { canvas.clipOutRect(it.left, it.top, it.right, it.bottom) }
        if (!capturing) segment(canvas, stroke, stroke.points.lastOrNull(), point)
        if (capturing) capture.add(transaction, InkPoint(x, y))
        canvas.restore(); stroke.points += point
    }
    private fun scheduleFrame(token: Long) { pendingToken = token; frameSession = session; pendingFrame = true; invalidate() }
    fun showPacket(packet: NativePacket) {
        val map = session?.coordinateMap ?: return
        val pos = IntArray(2); getLocationOnScreen(pos)
        callbackPoints = packet.points.map { map.toScreen(InkPoint(it.x, it.y)).let { p -> InkPoint(p.x - pos[0], p.y - pos[1]) } }
        invalidate()
    }
    fun receiveNative(stroke: RenderedStroke, arrivalNanos: Long) {
        if (pressureMode) capture.offer(stroke, arrivalNanos)
    }
    fun cancelStroke() {
        removeCallbacks(finishCapture); capture.clear(); captured.clear(); capturing = false
        current = null; rebuild(); invalidate()
    }
    fun clear() { cancelStroke(); strokes.clear(); callbackPoints = emptyList(); strokeCount = 0; rebuild(); save(); invalidate() }
    fun clearPacketMarkers() { callbackPoints = emptyList(); invalidate() }
    override fun performClick(): Boolean { super.performClick(); return true }
    private fun save() {
        val array = JSONArray()
        strokes.forEach { stroke ->
            array.put(JSONObject().put("width", stroke.width).put("points", JSONArray().apply {
                stroke.points.forEach { put(JSONArray().put(it.x).put(it.y)) }
            }).apply {
                stroke.nativeInk?.let { native -> put("native", JSONArray().apply {
                    native.points.forEach { put(JSONArray().put(it.x).put(it.y).put(it.width)) }
                }) }
            })
        }
        val text = JSONObject().put("schemaVersion", 2).put("width", docW).put("height", docH).put("strokes", array).toString()
        storeWorker.execute {
            try {
                val temp = File(store.parentFile, "strokes.tmp"); temp.writeText(text)
                check(temp.renameTo(store)) { "Atomic stroke replacement failed" }
                recording.event("strokesSaved", JSONObject().put("count", array.length()))
            } catch (e: Exception) { recording.event("strokeSaveFailed", JSONObject().put("error", e.toString())) }
        }
    }
    fun finish() { cancelStroke(); storeWorker.shutdown() }
}
