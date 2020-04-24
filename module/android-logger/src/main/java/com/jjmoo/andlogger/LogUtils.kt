package com.jjmoo.andlogger

import android.util.Log
import org.slf4j.ILoggerFactory

/**
 * @author Zohn
 */
@Suppress("unused")
object LogUtils {
    private const val TAG_MAX_LENGTH = 40

    @JvmStatic
    var prefix = "JmLog/"

    @JvmStatic
    var logDebug = true

    @JvmStatic
    var factory = ILoggerFactory { name ->
        val tag = prefix + name
        check(tag.length <= TAG_MAX_LENGTH)
        AndroidLoggerAdapter(tag)
    }

    @JvmStatic
    var logger: (Int, String, String) -> Unit = { priority, tag, msg ->
        Log.println(priority, tag, msg)
    }

    @JvmStatic
    fun getCaller(): String {
        val stack = Thread.currentThread().stackTrace
        return stack[4].simpleString() + " <-- " + stack[5].simpleString()
    }

    @JvmStatic
    fun getCaller(depth: Int): String {
        check(depth >= 1)
        val stack = Thread.currentThread().stackTrace
        val sb = StringBuilder(stack[4].simpleString())
        for (d in 2..depth) {
            sb.append(" <-- ").append(stack[d + 3].simpleString())
        }
        return sb.toString()
    }

    private fun StackTraceElement.simpleString() = "$methodName ($fileName:$lineNumber)"
}