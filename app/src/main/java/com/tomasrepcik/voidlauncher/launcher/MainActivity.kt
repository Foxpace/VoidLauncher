package com.tomasrepcik.voidlauncher.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tomasrepcik.voidlauncher.launcher.root.VoidLauncherApp
import com.tomasrepcik.voidlauncher.design.theme.VoidLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VoidLauncherTheme {
                VoidLauncherApp()
            }
        }
    }
}
