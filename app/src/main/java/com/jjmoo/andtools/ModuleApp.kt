package com.jjmoo.andtools

import android.app.Application
import com.jjmoo.andutil.JmLog
import com.jjmoo.appjoint.annotation.ModuleSpec

/**
 * @author Zohn
 */
@ModuleSpec
class ModuleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        JmLog.Utils.enterMethod()
    }
}