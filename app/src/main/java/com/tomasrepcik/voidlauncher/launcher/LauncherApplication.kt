package com.tomasrepcik.voidlauncher.launcher

import android.app.Application
import com.tomasrepcik.voidlauncher.launcher.launcherModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LauncherApplication)
            modules(launcherModule)
        }
    }
}
