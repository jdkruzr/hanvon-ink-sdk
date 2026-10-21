package dev.hanvon.ink

data class NativePoint(val x: Float, val y: Float, val renderedWidth: Float)
/** A packet, not necessarily a unique stroke. arrivalNanos is not a native sampling timestamp. */
data class NativePacket(val reportedPenUp: Boolean, val arrivalNanos: Long, val points: List<NativePoint>) {
    companion object {
        fun parse(reportedPenUp: Boolean, arrivalNanos: Long, values: FloatArray): NativePacket {
            require(values.isNotEmpty()) { "Empty native packet" }
            val n = values[0]
            require(n.isFinite() && n >= 0 && n <= 80_000 && n == n.toInt().toFloat()) { "Invalid point count" }
            require(values.size == 1 + n.toInt() * 3) { "Point count/length mismatch" }
            require(values.all { it.isFinite() }) { "Non-finite native point" }
            val points = (0 until n.toInt()).map {
                val offset = 1 + it * 3
                require(values[offset + 2] >= 0) { "Negative native width" }
                NativePoint(values[offset], values[offset + 1], values[offset + 2])
            }
            return NativePacket(reportedPenUp, arrivalNanos, points)
        }
    }
}
