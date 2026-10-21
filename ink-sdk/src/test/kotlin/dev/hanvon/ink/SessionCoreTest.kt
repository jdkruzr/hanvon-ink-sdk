package dev.hanvon.ink

import org.junit.Assert.*
import org.junit.Test

class SessionCoreTest {
    private class Fake : PenTransport {
        val calls = mutableListOf<Pair<String, List<Any>>>()
        var fail: String? = null
        override fun init(callback: (Boolean, Long, FloatArray) -> Unit): Long { calls += "init" to emptyList(); return 41L }
        override fun call(name: String, vararg args: Any) {
            calls += name to args.toList()
            if (name == fail) error("service gone")
        }
        override fun end(handle: Long) { calls += "end" to listOf(handle) }
    }
    @Test fun configuresInactiveAreaBeforeActivatingAndNeverEnablesOverlayGlobally() {
        val fake = Fake(); val core = SessionCore(fake, InkConfig(DrawingMode.NATIVE_PREVIEW))
        core.open { _, _, _ -> }
        assertEquals("setAreaActive" to listOf(41L, false), fake.calls[1])
        assertEquals("enablePen" to listOf(41L, false), fake.calls[2])
        core.configure(CoordinateMap(0, 0, 1860f, 2480f), InkRect(100f, 200f, 400f, 600f), InkRect(150f, 250f, 250f, 350f))
        assertTrue(fake.calls.contains("setExceptionArea" to listOf<Any>(41L, 250, 1610, 350, 1710)))
        core.activate()
        assertEquals("enablePen" to listOf(41L, false), fake.calls.last())
        assertFalse(fake.calls.any { it.first == "setOverlay" || it.first == "resetData" })
    }
    @Test fun teardownContinuesAfterSetterFailsAndIsIdempotent() {
        val fake = Fake(); val core = SessionCore(fake, InkConfig())
        core.open { _, _, _ -> }; fake.fail = "enablePen"
        assertEquals(1, core.close().size)
        assertEquals("end" to listOf(41L), fake.calls.last())
        core.close()
        assertEquals(1, fake.calls.count { it.first == "end" })
        assertFalse(fake.calls.any { it.first == "resetData" })
    }
    @Test fun partiallyConfiguredHandleCanStillBeReleased() {
        val fake = Fake().apply { fail = "setPen" }; val core = SessionCore(fake, InkConfig())
        assertThrows(IllegalStateException::class.java) { core.open { _, _, _ -> } }
        core.close()
        assertEquals("end" to listOf(41L), fake.calls.last())
    }
    @Test fun closedSessionCannotReopen() {
        val core = SessionCore(Fake(), InkConfig()); core.close()
        assertThrows(IllegalStateException::class.java) { core.open { _, _, _ -> } }
    }
    @Test fun callbackModeReceivesWithoutWaitingForAndroidDown() {
        val fake = Fake(); val core = SessionCore(fake, InkConfig(DrawingMode.NATIVE_CALLBACK))
        core.open { _, _, _ -> }; core.activate()
        assertTrue(fake.calls.contains("setDrawStatus" to listOf<Any>(41L, 0)))
        assertEquals("enablePen" to listOf(41L, true), fake.calls.last())
    }
    @Test fun defaultUsesThePhysicallyTestedContinuousModeZero() {
        val fake = Fake(); val core = SessionCore(fake, InkConfig())
        core.open { _, _, _ -> }; core.activate()
        assertEquals(DrawingMode.NATIVE_CALLBACK, InkConfig().mode)
        assertTrue(fake.calls.contains("setDrawStatus" to listOf<Any>(41L, 0)))
        assertEquals("enablePen" to listOf(41L, true), fake.calls.last())
    }
    @Test fun lateFramesAndTimeoutsCannotClearANewStroke() {
        val gate = HandoffGate()
        val first = gate.begin(); gate.end(100)
        assertNull(gate.delay(first, 2000, 1000))
        gate.frame(first)
        assertEquals(900L, gate.delay(first, 200, 1000))
        assertEquals(0L, gate.delay(first, 1100, 1000))
        val second = gate.begin()
        gate.frame(first)
        assertNull(gate.delay(first, 3000, 1000))
        assertNull(gate.delay(second, 3000, 1000))
        gate.end(3100); gate.frame(second); gate.invalidate()
        assertNull(gate.delay(second, 5000, 1000))
    }
    @Test fun duplicateFrameCommitDoesNotClearTwice() {
        val gate = HandoffGate(); val token = gate.begin(); gate.end(100); gate.frame(token)
        gate.cleared(token); gate.frame(token)
        assertNull(gate.delay(token, 2000, 1000))
    }
    @Test fun markerConfigurationCanReturnToBlackPressurePenWithoutReopening() {
        val fake = Fake(); val core = SessionCore(fake, InkConfig())
        core.open { _, _, _ -> }; core.activate(); core.suspend()
        core.brush(NativeBrush.PENCIL); core.previewColor(NativePreviewColor.DARK_GRAY); core.width(30)
        core.activate(); core.suspend()
        core.brush(NativeBrush.PRESSURE_PEN); core.previewColor(NativePreviewColor.BLACK); core.width(6)
        core.activate()
        assertEquals(listOf(1, 3, 1), fake.calls.filter { it.first == "setPenColor" }.map { it.second.last() })
        assertEquals(listOf(15, 15, 6), fake.calls.filter { it.first == "setPen" }.map { it.second.last() })
        assertEquals(1, fake.calls.count { it.first == "init" })
        assertFalse(fake.calls.any { it.first == "resetData" })
        core.close()
    }
}
