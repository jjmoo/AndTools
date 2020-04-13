package com.jjmoo.andtools

import android.app.Application
import com.jjmoo.andlogger.LogUtils
import com.jjmoo.appjoint.annotation.AppSpec
import org.slf4j.LoggerFactory

/**
 * @author Zohn
 */
@AppSpec
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogUtils.prefix = "~~~~~~"
        LoggerFactory.getLogger("MyApp").info("onCreate")
    }
}