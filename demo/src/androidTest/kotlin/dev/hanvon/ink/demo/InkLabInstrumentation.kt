package dev.hanvon.ink.demo

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import dev.hanvon.ink.HanvonInk
import org.json.JSONObject
import java.io.File

/** Synthetic host-rendering test, NOT a test of physical input, native ink, or visible latency. */
class InkLabInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun launch(): Activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        .putExtra("mode", "baseline").putExtra("testRun", "synthetic-100-stroke-retention"))
    private fun canvas(view: View): InkCanvas? {
        if (view is InkCanvas) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) canvas(view.getChildAt(i))?.let { return it }
        return null
    }
    override fun onStart() {
        var activity: Activity? = null
        try {
            val capability = HanvonInk.probe(targetContext)
            check(!capability.available) { "Run this destructive demo-data fixture on the non-Hanvon emulator only" }
            activity = launch(); waitForIdleSync()
            val surface = checkNotNull(canvas(activity.window.decorView))
            val initialWidth = surface.width
            val initialHeight = surface.height
            runOnMainSync { surface.clear() }
            for (index in 0 until 100) {
                runOnMainSync {
                    val start = SystemClock.uptimeMillis()
                    val x = surface.width * (.1f + (index % 10) * .07f)
                    val y = surface.height * (.1f + (index / 10) * .07f)
                    val props = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS }
                    for ((step, action) in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP).withIndex()) {
                        val coords = MotionEvent.PointerCoords().apply { this.x = x + step * 3; this.y = y + step * 3; pressure = .5f; size = .1f }
                        val event = MotionEvent.obtain(start, start + step, action, 1, arrayOf(props), arrayOf(coords),
                            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
                        try { surface.onTouchEvent(event) } finally { event.recycle() }
                    }
                }
                waitForIdleSync()
            }
            check(surface.strokeCount == 100) { "Retained stroke count ${surface.strokeCount}" }
            check(surface.width == initialWidth && surface.height == initialHeight) { "Diagnostic text resized the drawing surface" }
            val file = File(targetContext.filesDir, "strokes.json")
            val deadline = SystemClock.uptimeMillis() + 10_000
            while (SystemClock.uptimeMillis() < deadline && runCatching { JSONObject(file.readText()).getJSONArray("strokes").length() }.getOrDefault(0) != 100) {
                Thread.sleep(50)
            }
            check(JSONObject(file.readText()).getJSONArray("strokes").length() == 100) { "Persistence incomplete" }
            runOnMainSync { activity?.finish() }; waitForIdleSync()
            activity = launch(); waitForIdleSync()
            val restored = checkNotNull(canvas(activity.window.decorView))
            check(restored.strokeCount == 100) { "Restored ${restored.strokeCount}, expected 100" }
            finish(Activity.RESULT_OK, Bundle().apply {
                putString("stream", "PASS: 100 synthetic stylus strokes retained and restored. Native capability=${capability.available}; ${capability.reason}. This does not validate physical ink.")
                putInt("strokes", 100)
            })
        } catch (e: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "FAIL: ${e.stackTraceToString()}") })
        } finally { activity?.let { runOnMainSync { it.finish() } } }
    }
}
