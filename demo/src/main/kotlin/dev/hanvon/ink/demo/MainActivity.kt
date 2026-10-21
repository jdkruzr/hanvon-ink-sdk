package dev.hanvon.ink.demo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import dev.hanvon.ink.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var recording: Recording
    private lateinit var canvas: InkCanvas
    private lateinit var status: TextView
    private lateinit var counts: TextView
    private lateinit var capability: CapabilityReport
    private lateinit var modes: Spinner
    private var session: InkSession? = null
    private var resumed = false
    private var selected = 3
    private var generation = 0
    private var statusText = "Starting"
    private val ui = Handler(Looper.getMainLooper())
    private var destroyed = false
    private val tick = object : Runnable {
        override fun run() {
            if (destroyed) return
            counts.text = "Events ${recording.count.get()} · motion ${recording.motionCount.get()} · keys ${recording.keyCount.get()} · packets ${recording.packetCount.get()} · strokes ${canvas.strokeCount} · dropped ${recording.dropped.get()}" +
                (recording.ioFailure?.let { "\nRecording failed: $it" } ?: "")
            recording.flush()
            ui.postDelayed(this, 1000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capability = HanvonInk.probe(this)
        recording = Recording(this, capability)
        selected = savedInstanceState?.getInt("mode") ?: getPreferences(MODE_PRIVATE).getInt("mode", if (capability.available) 3 else 0)
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            selected = when (intent.getStringExtra("mode")) { "baseline" -> 0; "preview" -> 1; "callback" -> 2; "native" -> 3; else -> selected }
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(-1); fitsSystemWindows = true }
        val title = TextView(this).apply { text = "Hanvon Ink Lab · experimental"; textSize = 22f; setPadding(16, 12, 16, 4) }
        root.addView(title)
        // Stable height: diagnostic text must never move a live native drawing area.
        status = TextView(this).apply { textSize = 15f; setLines(3); ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(16, 0, 16, 4) }; root.addView(status)
        counts = TextView(this).apply { textSize = 13f; setLines(2); ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(16, 0, 16, 4) }; root.addView(counts)
        modes = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Android baseline", "Mode 3 preview (experimental)", "Native callback inspection", "Native ink + retained drawing"))
            setSelection(selected)
        }
        root.addView(modes)
        val row = LinearLayout(this)
        fun button(text: String, action: () -> Unit) { row.addView(Button(this).apply { this.text = text; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, 56.dp, 1f)) }
        button("Clear") { stopSession(); canvas.clear(); startSession(); recording.event("clearCanvas") }
        button("Rotate") {
            requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        button("Observe") { observationDialog() }
        button("Export") {
            recording.event("exportRequested", JSONObject().put("displayMode", Recording.readDisplayMode(this)))
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "application/zip"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_TITLE, "hanvon-ink-${recording.directory.name}.zip")
            }, 20)
        }
        root.addView(row)
        val options = LinearLayout(this)
        options.addView(CheckBox(this).apply {
            text = "Excluded center"; isChecked = savedInstanceState?.getBoolean("excluded") ?: false
            setOnCheckedChangeListener { _, enabled -> canvas.exclusionEnabled = enabled; recording.event("exclusion", JSONObject().put("enabled", enabled)) }
        })
        options.addView(TextView(this).apply { text = "Width"; gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(60.dp, 48.dp))
        val widthSlider = SeekBar(this).apply { max = 31; progress = (savedInstanceState?.getInt("width") ?: 4) - 1 }
        options.addView(widthSlider, LinearLayout.LayoutParams(0, 48.dp, 1f))
        root.addView(options)
        canvas = InkCanvas(this, recording).apply {
            widthPx = widthSlider.progress + 1
            exclusionEnabled = savedInstanceState?.getBoolean("excluded") ?: false
            onStroke = { counts.text = "Stroke ${canvas.strokeCount} recorded · see counters below on next update" }
        }
        root.addView(canvas, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        modes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == selected) return
                selected = position; getPreferences(MODE_PRIVATE).edit().putInt("mode", selected).apply()
                stopSession(); startSession()
            }
        }
        widthSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) { session?.suspendDrawing() }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { canvas.widthPx = progress + 1 }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                session?.setWidth(canvas.widthPx); session?.resumeDrawing()
                recording.event("penWidth", JSONObject().put("pixels", canvas.widthPx))
            }
        })
        recording.event("activityCreated", JSONObject().put("restoredStrokes", canvas.strokeCount).put("requestedMode", selected))
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) intent.getStringExtra("testRun")?.let {
            recording.event("syntheticTest", JSONObject().put("name", it).put("physicalInkValidated", false))
        }
        ui.post(tick)
        val cycles = if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) intent.getIntExtra("cycles", 0).coerceIn(0, 50) else 0
        if (cycles > 0) ui.postDelayed({ cycle(cycles, 1) }, 1000)
    }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
    private fun displayStatus(text: String) {
        statusText = text
        status.text = "${capability.model} · interface ${if (capability.available) "available" else "unavailable"} · physical result unverified\n$text"
    }
    private fun startSession() {
        if (!resumed || isInMultiWindowMode || session != null) return
        canvas.clearPacketMarkers()
        val thisGeneration = ++generation
        recording.event("mode", JSONObject().put("selected", selected).put("displayMode", Recording.readDisplayMode(this)))
        if (selected == 0) { displayStatus("Android baseline"); return }
        if (!capability.available) { displayStatus("Android fallback: ${capability.reason}"); return }
        try {
            val created = HanvonInk.attach(canvas, InkConfig(if (selected == 1) DrawingMode.NATIVE_PREVIEW else DrawingMode.NATIVE_CALLBACK, canvas.widthPx), object : InkListener {
                override fun onState(state: SessionState, detail: String) {
                    recording.event("sessionState", JSONObject().put("generation", thisGeneration).put("state", state.name).put("detail", detail))
                    if (generation != thisGeneration || destroyed) return
                    val name = when (selected) { 1 -> "Mode 3 preview"; 2 -> "Callback inspection"; else -> "Native ink" }
                    displayStatus("$name: $state · $detail")
                    if (state == SessionState.FAILED) {
                        stopSession(); displayStatus("Android fallback: native session failed · $detail")
                    }
                }
                override fun onDiagnostic(operation: String, detail: String, elapsedNanos: Long) {
                    recording.event("service", JSONObject().put("generation", thisGeneration).put("operation", operation)
                        .put("detail", detail).put("durationNanos", elapsedNanos))
                    android.util.Log.i("HanvonInkLab", "$operation: $detail")
                }
                override fun onNativePacket(packet: NativePacket) {
                    if (generation != thisGeneration) return
                    recording.packetCount.incrementAndGet()
                    val points = JSONArray()
                    packet.points.forEach { points.put(JSONArray().put(it.x).put(it.y).put(it.renderedWidth)) }
                    recording.event("nativePacket", JSONObject().put("reportedPenUp", packet.reportedPenUp)
                        .put("callbackArrivalNanos", packet.arrivalNanos).put("points", points))
                    if (selected == 2) canvas.showPacket(packet)
                }
            })
            session = created; canvas.session = created; canvas.updateExclusion(); created.setForeground(true)
        } catch (e: Exception) {
            displayStatus("Android fallback: ${e.message}")
            recording.event("attachFailed", JSONObject().put("error", e.toString()))
        }
    }
    private fun stopSession() {
        generation++
        canvas.cancelStroke(); session?.close(); session = null; canvas.session = null
    }
    override fun onResume() {
        super.onResume(); resumed = true; canvas.post { if (resumed && !destroyed) startSession() }
        recording.event("resumed")
    }
    override fun onPause() {
        resumed = false; stopSession(); recording.event("paused"); recording.flush(); super.onPause()
    }
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        stopSession()
        if (!isInMultiWindowMode) startSession() else displayStatus("Android baseline: native suspended in multiwindow")
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("mode", selected); outState.putInt("width", canvas.widthPx); outState.putBoolean("excluded", canvas.exclusionEnabled)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        destroyed = true; ui.removeCallbacksAndMessages(null); stopSession(); canvas.finish(); recording.close(); super.onDestroy()
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::recording.isInitialized) recording.motion("activityTouch", event)
        return super.dispatchTouchEvent(event)
    }
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (::recording.isInitialized) recording.motion("activityGeneric", event)
        return super.dispatchGenericMotionEvent(event)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::recording.isInitialized) recording.key(event)
        return super.dispatchKeyEvent(event)
    }
    @Deprecated("Legacy result API keeps the demo dependency-free")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 20 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val output = contentResolver.openOutputStream(uri) ?: error("Cannot open export destination")
            recording.export(output) { error -> runOnUiThread { if (!destroyed) Toast.makeText(this, error ?: "Recording exported", Toast.LENGTH_LONG).show() } }
        } catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
    }
    private fun observationDialog() {
        session?.suspendDrawing()
        val choices = arrayOf("Native ink visibly leads Android", "No visible native acceleration", "Retained ink survives overlay clearing",
            "Ink disappears or differs after clearing", "Clipping correct", "Ink escaped drawing bounds", "Button/eraser test completed")
        AlertDialog.Builder(this).setTitle("Record a physical observation")
            .setItems(choices) { _, index -> recording.event("humanObservation", JSONObject().put("observation", choices[index]).put("mode", selected)) }
            .setNegativeButton("Cancel", null).setOnDismissListener { session?.resumeDrawing() }.show()
    }
    private fun cycle(total: Int, current: Int) {
        if (!resumed || destroyed) { recording.event("cycleAborted"); return }
        stopSession()
        recording.event("cycle", JSONObject().put("index", current).put("total", total))
        startSession()
        if (current < total) ui.postDelayed({ cycle(total, current + 1) }, 500)
        else ui.postDelayed({ recording.event("cyclesComplete", JSONObject().put("count", total)); recording.flush() }, 500)
    }
}
