package com.jjmoo.andtools

import android.app.Application
import com.jjmoo.andutil.JmLog
import com.jjmoo.appjoint.annotation.AppSpec

/**
 * @author Zohn
 */
@AppSpec
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        JmLog.setup(this, "AndToolsTest", true, false)
        JmLog.Utils.enterMethod()
    }
}