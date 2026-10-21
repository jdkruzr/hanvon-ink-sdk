package dev.hanvon.ink

import android.content.Context
import android.os.Build
import android.os.SystemClock
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

data class CapabilityReport(
    val available: Boolean,
    val model: String,
    val fingerprint: String,
    val origin: Int?,
    val methods: Map<String, Boolean>,
    val reason: String?,
    val validation: String = "unverified",
)

internal interface PenTransport {
    fun init(callback: (Boolean, Long, FloatArray) -> Unit): Long
    fun call(name: String, vararg args: Any)
    fun end(handle: Long)
}

internal class ReflectionTransport(private val manager: Any, private val listenerType: Class<*>,
    private val methods: Map<String, Method>) : PenTransport {
    private var listener: Any? = null
    private fun invoke(name: String, vararg args: Any): Any? = try {
        methods.getValue(name).invoke(manager, *args)
    } catch (e: InvocationTargetException) { throw e.targetException }

    override fun init(callback: (Boolean, Long, FloatArray) -> Unit): Long {
        check(listener == null)
        listener = Proxy.newProxyInstance(listenerType.classLoader, arrayOf(listenerType)) { proxy, method, args ->
            when (method.name) {
                "onPenTouchUpStatus" -> {
                    val points = args?.getOrNull(1) as? FloatArray ?: floatArrayOf()
                    callback(args?.getOrNull(0) as? Boolean ?: false, SystemClock.elapsedRealtimeNanos(), points.copyOf())
                    null
                }
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                "toString" -> "HanvonInkListener"
                else -> null
            }
        }
        return (invoke("initService", 0, 0, 0, 0, listener!!) as Number).toLong()
    }
    override fun call(name: String, vararg args: Any) { invoke(name, *args) }
    override fun end(handle: Long) {
        val callback = listener ?: return
        try { invoke("endService", handle, callback) } finally { listener = null }
    }

    companion object {
        fun discover(context: Context): Pair<CapabilityReport, ReflectionTransport?> {
            val found = linkedMapOf<String, Boolean>()
            var origin: Int? = null
            return try {
                val manager = context.getSystemService("hvpen") ?: error("hvpen service unavailable")
                val listener = Class.forName("android.os.HvPenDrawListener")
                val i = Int::class.javaPrimitiveType!!
                val l = Long::class.javaPrimitiveType!!
                val b = Boolean::class.javaPrimitiveType!!
                val signatures = linkedMapOf(
                    "initService" to arrayOf(i, i, i, i, listener), "endService" to arrayOf(l, listener),
                    "setDrawArea" to arrayOf(l, i, i, i, i), "setExceptionArea" to arrayOf(l, i, i, i, i),
                    "setDrawStatus" to arrayOf(l, i), "setPen" to arrayOf(l, i),
                    "setPenWidth" to arrayOf(l, i), "setPenColor" to arrayOf(l, i),
                    "enablePen" to arrayOf(l, b), "setAreaActive" to arrayOf(l, b),
                    "setOrientation" to arrayOf(i), "resetData" to emptyArray(), "getScreenOrgPos" to emptyArray(),
                )
                val methods = mutableMapOf<String, Method>()
                signatures.forEach { (name, params) ->
                    val method = runCatching { manager.javaClass.getMethod(name, *params) }.getOrNull()
                    found[name] = method != null
                    if (method != null) methods[name] = method
                }
                check(found.values.all { it }) { "Missing methods: ${found.filterValues { !it }.keys}" }
                origin = (methods.getValue("getScreenOrgPos").invoke(null) as Number).toInt()
                check(origin in 0..2) { "Unknown coordinate origin $origin" }
                CapabilityReport(true, Build.MODEL, Build.FINGERPRINT, origin, found, null) to
                    ReflectionTransport(manager, listener, methods)
            } catch (e: Throwable) {
                CapabilityReport(false, Build.MODEL, Build.FINGERPRINT, origin, found,
                    "${e.javaClass.simpleName}: ${e.message}") to null
            }
        }
    }
}
