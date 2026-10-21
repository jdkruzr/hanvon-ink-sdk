package dev.hanvon.ink

import org.junit.Assert.*
import org.junit.Test

class GeometryTest {
    @Test fun m10PortraitMatchesRecoveredAxisSwapAndReflection() {
        val map = CoordinateMap(0, 0, 1860f, 2480f)
        assertEquals(InkPoint(200f, 1760f), map.toNative(InkPoint(100f, 200f)))
        assertEquals(InkRect(200f, 1460f, 600f, 1760f), map.toNative(InkRect(100f, 200f, 400f, 600f)))
        assertEquals(11, map.orientation)
    }
    @Test fun otherOriginsHaveDistinctPortraitConventions() {
        assertEquals(InkPoint(2280f, 100f), CoordinateMap(1, 0, 1860f, 2480f).toNative(InkPoint(100f, 200f)))
        assertEquals(InkPoint(100f, 200f), CoordinateMap(2, 0, 1860f, 2480f).toNative(InkPoint(100f, 200f)))
    }
    @Test fun everyOriginAndRotationRoundTripsEdgesAndFractionalPoints() {
        for (origin in 0..2) for (rotation in 0..3) {
            val w = if (rotation % 2 == 0) 1860f else 2480f
            val h = if (rotation % 2 == 0) 2480f else 1860f
            val map = CoordinateMap(origin, rotation, w, h)
            for (point in listOf(InkPoint(0f, 0f), InkPoint(w, h), InkPoint(27.5f, 182.25f), InkPoint(w, 0f), InkPoint(0f, h))) {
                assertEquals(point, map.toScreen(map.toNative(point)))
            }
            val rect = map.toNative(InkRect(20f, 50f, 200f, 350f))
            assertEquals(180f * 300f, (rect.right - rect.left) * (rect.bottom - rect.top), .01f)
        }
    }
    @Test fun rejectsUnknownGeometry() {
        assertThrows(IllegalArgumentException::class.java) { CoordinateMap(8, 0, 100f, 100f) }
        assertThrows(IllegalArgumentException::class.java) { CoordinateMap(0, 4, 100f, 100f) }
        assertThrows(IllegalArgumentException::class.java) { CoordinateMap(0, 0, Float.POSITIVE_INFINITY, 100f) }
        assertThrows(IllegalArgumentException::class.java) { InkRect(1f, 0f, 0f, 10f) }
    }
}
