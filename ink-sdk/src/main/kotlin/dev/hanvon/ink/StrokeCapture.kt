package dev.hanvon.ink

import kotlin.math.hypot

/** Conservative experimental association. A host transaction ID is not a firmware stroke ID.
 * Call from one thread; use monotonic nanoseconds for all times. Finish a batch after a quiet
 * interval, then apply results to the original document/revision. Never use callback arrival
 * time as the pen sampling timestamp. Ambiguous packets produce fallback, not guessed ink.
 */
class StrokeCapture(private val deadlineNanos: Long = 1_000_000_000L) {
    data class Result(val id: Long, val stroke: RenderedStroke?, val reason: String)
    private data class Input(val id: Long, val began: Long, val points: MutableList<InkPoint>, var ended: Long? = null)
    private data class Packet(val stroke: RenderedStroke, val arrived: Long)
    private val inputs = linkedMapOf<Long, Input>()
    private val packets = mutableListOf<Packet>()
    private var overflow = false
    init { require(deadlineNanos > 0) }
    fun begin(id: Long, now: Long, point: InkPoint): Boolean {
        require(id !in inputs) { "Transaction ID reused in capture batch" }
        if (inputs.size >= 256) { overflow = true; return false }
        inputs[id] = Input(id, now, mutableListOf(point))
        return true
    }
    fun add(id: Long, point: InkPoint) {
        inputs[id]?.let { if (it.points.size < 80_000) it.points += point else overflow = true }
    }
    fun end(id: Long, now: Long) { inputs[id]?.ended = now }
    fun offer(stroke: RenderedStroke, arrivalNanos: Long) {
        if (packets.size < 512 && packets.sumOf { it.stroke.points.size } + stroke.points.size <= 320_000)
            packets += Packet(stroke, arrivalNanos)
        else overflow = true
    }
    /** Only complete a fully ended batch after the callback deadline. Caller retains raw input until then. */
    // force closes a host ownership boundary using only packets already received.
    fun finish(now: Long, force: Boolean = false): List<Result> {
        if (inputs.values.any { it.ended == null || (!force && now - it.ended!! < deadlineNanos) }) return emptyList()
        val matches = packets.map { packet -> packet to inputs.values.filter { matches(it, packet) } }
        val results = inputs.values.map { input ->
            val candidates = matches.filter { (_, owners) -> owners.any { it.id == input.id } }
            val unique = candidates.filter { it.second.size == 1 }.map { it.first.stroke }.distinctBy { it.points }
            when {
                overflow -> Result(input.id, null, "capture budget exceeded")
                candidates.any { it.second.size != 1 } -> Result(input.id, null, "ambiguous packet ownership")
                unique.size == 1 -> Result(input.id, unique.single(), "unique geometry/time match; experimental")
                unique.isEmpty() -> Result(input.id, null, "missing or mismatched native packet")
                else -> Result(input.id, null, "multiple different native packets")
            }
        }
        clear()
        return results
    }
    fun clear() { inputs.clear(); packets.clear(); overflow = false }
    private fun matches(input: Input, packet: Packet): Boolean {
        val ended = input.ended ?: return false
        if (packet.arrived < input.began || packet.arrived > ended + deadlineNanos) return false
        val points = packet.stroke.points
        val a = input.points.first(); val b = input.points.last()
        // Arrival is not completion. Check both ends and the actual path; never use ordering alone.
        if (hypot(points.first().x - a.x, points.first().y - a.y) > 16f ||
            hypot(points.last().x - b.x, points.last().y - b.y) > 48f) return false
        // Bounded representative path checks against a bounded Android polyline.
        val host = input.points.filterIndexed { i, _ -> i % maxOf(1, input.points.size / 256) == 0 }.toMutableList()
        if (host.last() != b) host += b
        return points.filterIndexed { i, _ -> i % maxOf(1, points.size / 64) == 0 }.all { p ->
            // Native smoothing can retain a lift-off tail past Android's last contact.
            // Use the same bounded endpoint allowance as above, not a wider whole-path tolerance.
            if (hypot(p.x - b.x, p.y - b.y) <= 48f) true
            else if (host.size == 1) hypot(p.x - a.x, p.y - a.y) <= 24f
            else host.zipWithNext().any { (v, w) ->
                val dx = w.x - v.x; val dy = w.y - v.y
                val den = dx * dx + dy * dy
                val t = if (den == 0f) 0f else (((p.x - v.x) * dx + (p.y - v.y) * dy) / den).coerceIn(0f, 1f)
                hypot(p.x - v.x - t * dx, p.y - v.y - t * dy) <= 24f
            }
        }
    }
}
