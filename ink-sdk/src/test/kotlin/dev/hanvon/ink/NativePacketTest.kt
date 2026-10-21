package dev.hanvon.ink

import org.junit.Assert.*
import org.junit.Test

class NativePacketTest {
    @Test fun preservesWidthsAndCallbackFlagWithoutInventingPressureOrTime() {
        val packet = NativePacket.parse(false, 123L, floatArrayOf(2f, 10f, 20f, 3f, 30f, 40f, 5f))
        assertFalse(packet.reportedPenUp)
        assertEquals(123L, packet.arrivalNanos)
        assertEquals(listOf(NativePoint(10f, 20f, 3f), NativePoint(30f, 40f, 5f)), packet.points)
    }
    @Test fun zeroPointsIsAValidPacketNotAnInventedStroke() {
        assertTrue(NativePacket.parse(true, 0L, floatArrayOf(0f)).points.isEmpty())
    }
    @Test fun rejectsMalformedPackets() {
        for (input in listOf(floatArrayOf(), floatArrayOf(Float.NaN), floatArrayOf(-1f), floatArrayOf(.5f),
            floatArrayOf(80_001f), floatArrayOf(1f), floatArrayOf(1f, 2f, 3f, Float.POSITIVE_INFINITY),
            floatArrayOf(1f, 2f, 3f, -1f), floatArrayOf(0f, 1f))) {
            assertThrows(IllegalArgumentException::class.java) { NativePacket.parse(true, 0, input) }
        }
    }
}
