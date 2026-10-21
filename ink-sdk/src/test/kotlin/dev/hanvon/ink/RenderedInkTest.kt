package dev.hanvon.ink

import org.junit.Assert.*
import org.junit.Test

class RenderedInkTest {
    private fun stroke(x: Float = 0f) = RenderedStroke(listOf(RenderedPoint(x, 0f, 2f), RenderedPoint(x + 10, 0f, 6f)))
    private fun begin(c: StrokeCapture, id: Long, time: Long, x: Float = 0f) {
        c.begin(id, time, InkPoint(x, 0f)); c.add(id, InkPoint(x + 10, 0f)); c.end(id, time + 10)
    }
    @Test fun immutableWidthsTransformAsDiameters() {
        val points = mutableListOf(RenderedPoint(2f, 3f, 4f))
        val stroke = RenderedStroke(points); points.clear()
        assertEquals(RenderedPoint(5f, 8f, 8f), stroke.transformed(2f, 1f, 2f).points.single())
        assertThrows(UnsupportedOperationException::class.java) { (stroke.points as MutableList).clear() }
        assertThrows(IllegalArgumentException::class.java) { RenderedPoint(0f, 0f, Float.NaN) }
    }
    @Test fun callbacksBeforeAndroidUpAndDuplicatePacketsMakeOneResult() {
        val c = StrokeCapture(100)
        c.begin(1, 0, InkPoint(0f, 0f)); c.add(1, InkPoint(10f, 0f))
        c.offer(stroke(), 8); c.offer(stroke(), 9); c.end(1, 10)
        assertTrue(c.finish(109).isEmpty())
        val result = c.finish(110).single()
        assertEquals(1L, result.id); assertNotNull(result.stroke)
        assertTrue(c.finish(111).isEmpty())
    }
    @Test fun repeatedIdenticalStrokesAreAmbiguousNotMerged() {
        val c = StrokeCapture(100); begin(c, 1, 0); begin(c, 2, 20)
        c.offer(stroke(), 25); c.offer(stroke(), 31)
        val results = c.finish(130)
        assertEquals(2, results.size); assertTrue(results.all { it.stroke == null })
    }
    @Test fun rapidDistinctStrokesRemainDistinct() {
        val c = StrokeCapture(100); begin(c, 1, 0); begin(c, 2, 20, 100f)
        c.offer(stroke(100f), 31); c.offer(stroke(), 32)
        assertTrue(c.finish(130).all { it.stroke != null })
    }
    @Test fun lateMissingAndRevisionInvalidationPreserveFallback() {
        val c = StrokeCapture(100); begin(c, 1, 0); c.offer(stroke(), 111)
        assertNull(c.finish(120).single().stroke)
        begin(c, 2, 200); c.offer(stroke(), 211); c.clear()
        assertTrue(c.finish(1000).isEmpty())
    }
    @Test fun nativeDeliveryCanPrecedeAndroidDispatch() {
        val c = StrokeCapture(100)
        c.offer(stroke(), 15) // main thread receives native packet before dispatching queued Android input
        begin(c, 1, 0) // use event timestamp, NOT dispatch timestamp
        assertNotNull(c.finish(110).single().stroke)
    }
    @Test fun recordedM10PressurePacketsSurviveQueuedAndroidDispatch() {
        val c = StrokeCapture()
        javaClass.getResourceAsStream("/m10-pressure-queued-input.txt")!!.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val v = line.split(' ')
                when (v[0]) {
                    "B" -> c.begin(v[1].toLong(), v[2].toLong(), InkPoint(v[3].toFloat(), v[4].toFloat()))
                    "A" -> c.add(v[1].toLong(), InkPoint(v[2].toFloat(), v[3].toFloat()))
                    "E" -> c.end(v[1].toLong(), v[2].toLong())
                    "P" -> c.offer(RenderedStroke(v.drop(2).chunked(3).map {
                        RenderedPoint(it[0].toFloat(), it[1].toFloat(), it[2].toFloat())
                    }), v[1].toLong())
                }
            }
        }
        val results = c.finish(100_000_000_000)
        assertEquals(6, results.size)
        assertTrue(results.toString(), results.all { it.stroke != null })
    }
    @Test fun captureLimitIsExplicitAndAllRetainedTransactionsFallBack() {
        val c = StrokeCapture(100)
        repeat(256) { begin(c, it.toLong(), 0) }
        assertFalse(c.begin(999, 0, InkPoint(0f, 0f)))
        val results = c.finish(200)
        assertEquals(256, results.size); assertTrue(results.all { it.stroke == null })
    }
    @Test fun ownershipBoundaryDrainsKnownPacketsAndKeepsMissingInputAsFallback() {
        val c = StrokeCapture(100); begin(c, 1, 0); begin(c, 2, 20, 100f)
        c.offer(stroke(), 15)
        val result = c.finish(30, force = true)
        assertNotNull(result[0].stroke); assertNull(result[1].stroke)
        c.offer(stroke(100f), 40)
        assertTrue(c.finish(200).isEmpty())
    }
    @Test fun buttonsAreBitsAndFingerIsNotAButton() {
        val both = PenInput.classify(2, 32 or 64)
        assertTrue(both.primary && both.secondary); assertEquals(PenSignal.TIP, both.tool)
        assertTrue(PenInput.classify(6, 0).primary)
        assertEquals(PenSignal.REAR_ERASER, PenInput.classify(4, 32).tool)
        assertEquals(PenSignals(PenSignal.FINGER, false, false, false), PenInput.classify(1, 0))
        assertEquals(PenSignal.PEN_AS_FINGER, PenInput.classify(1, 0, 0x5002).tool)
        assertEquals(PenSignal.FINGER, PenInput.classify(1, 0, 0x1002).tool)
    }
}
