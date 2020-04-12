package com.jjmoo.andlogger

import android.util.Log
import com.jjmoo.andlogger.LogUtils.logDebug
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter

class AndroidLoggerAdapter(tag: String) : MarkerIgnoringBase() {
    init {
        name = tag
    }

    override fun isTraceEnabled() = isLoggable(Log.VERBOSE)

    override fun trace(msg: String) = log(Log.VERBOSE, msg, null)

    override fun trace(format: String, arg: Any) = formatAndLog(Log.VERBOSE, format, arg)

    override fun trace(format: String, arg1: Any, arg2: Any) = formatAndLog(Log.VERBOSE, format, arg1, arg2)

    override fun trace(format: String, vararg argArray: Any) = formatAndLog(Log.VERBOSE, format, *argArray)

    override fun trace(msg: String, t: Throwable) = log(Log.VERBOSE, msg, t)

    override fun isDebugEnabled() = isLoggable(Log.DEBUG)

    override fun debug(msg: String) = log(Log.DEBUG, msg, null)

    override fun debug(format: String, arg: Any) = formatAndLog(Log.DEBUG, format, arg)

    override fun debug(format: String, arg1: Any, arg2: Any) = formatAndLog(Log.DEBUG, format, arg1, arg2)

    override fun debug(format: String, vararg argArray: Any) = formatAndLog(Log.DEBUG, format, *argArray)

    override fun debug(msg: String, t: Throwable) = log(Log.VERBOSE, msg, t)

    override fun isInfoEnabled() = isLoggable(Log.INFO)

    override fun info(msg: String) = log(Log.INFO, msg, null)

    override fun info(format: String, arg: Any) = formatAndLog(Log.INFO, format, arg)

    override fun info(format: String, arg1: Any, arg2: Any) = formatAndLog(Log.INFO, format, arg1, arg2)

    override fun info(format: String, vararg argArray: Any) = formatAndLog(Log.INFO, format, *argArray)

    override fun info(msg: String, t: Throwable) = log(Log.INFO, msg, t)

    override fun isWarnEnabled() = isLoggable(Log.WARN)

    override fun warn(msg: String) = log(Log.WARN, msg, null)

    override fun warn(format: String, arg: Any) = formatAndLog(Log.WARN, format, arg)

    override fun warn(format: String, arg1: Any, arg2: Any) = formatAndLog(Log.WARN, format, arg1, arg2)

    override fun warn(format: String, vararg argArray: Any) = formatAndLog(Log.WARN, format, *argArray)

    override fun warn(msg: String, t: Throwable) = log(Log.WARN, msg, t)

    override fun isErrorEnabled() = isLoggable(Log.ERROR)

    override fun error(msg: String) = log(Log.ERROR, msg, null)

    override fun error(format: String, arg: Any) = formatAndLog(Log.ERROR, format, arg)

    override fun error(format: String, arg1: Any, arg2: Any) = formatAndLog(Log.ERROR, format, arg1, arg2)

    override fun error(format: String, vararg argArray: Any) = formatAndLog(Log.ERROR, format, *argArray)

    override fun error(msg: String, t: Throwable) = log(Log.ERROR, msg, t)

    private fun isLoggable(priority: Int) = if (priority <= Log.DEBUG) logDebug else Log.isLoggable(name, priority)

    private fun formatAndLog(priority: Int, format: String, vararg argArray: Any) {
        if (isLoggable(priority)) {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            logInternal(priority, ft.message, ft.throwable)
        }
    }

    private fun log(priority: Int, message: String, throwable: Throwable?) {
        if (isLoggable(priority)) logInternal(priority, message, throwable)
    }

    private fun logInternal(priority: Int, message: String, throwable: Throwable?) {
        if (null == throwable) LogUtils.logger(priority, name, message)
        else LogUtils.logger(priority, name, message + "\n${Log.getStackTraceString(throwable)}")
    }
}