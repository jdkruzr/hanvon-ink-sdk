package dev.hanvon.ink

/** Signals, not application tool assignments. Finger tool from a stylus source remains distinct from ordinary touchscreen input. */
enum class PenSignal { TIP, REAR_ERASER, PRIMARY, SECONDARY, TERTIARY, PEN_AS_FINGER, FINGER, UNKNOWN }
data class PenSignals(val tool: PenSignal, val primary: Boolean, val secondary: Boolean, val tertiary: Boolean)
object PenInput {
    fun classify(toolType: Int, buttons: Int, source: Int = 0): PenSignals = PenSignals(
        when (toolType) { 4 -> PenSignal.REAR_ERASER; 1 -> if (source and 0x4002 == 0x4002) PenSignal.PEN_AS_FINGER else PenSignal.FINGER; 2, 6 -> PenSignal.TIP; else -> PenSignal.UNKNOWN },
        toolType == 6 || buttons and 32 != 0, buttons and 64 != 0, buttons and 128 != 0)
}
