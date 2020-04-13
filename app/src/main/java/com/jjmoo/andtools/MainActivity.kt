package com.jjmoo.andtools

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jjmoo.appjoint.AppJoint

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppJoint.service(ModuleFunc::class.java)?.action()
    }
}
