package com.jjmoo.andtools

import com.jjmoo.appjoint.annotation.ServiceProvider
import org.slf4j.LoggerFactory

/**
 * @author Zohn
 */
@ServiceProvider
class ModuleFuncImpl : ModuleFunc {
    override fun action() {
        LoggerFactory.getLogger("ModuleFuncImpl").info("action")
    }
}