package com.kmpboilerplate.app

import android.app.Application
import com.kmpboilerplate.infrastructure.config.bootstrap
import org.koin.android.ext.koin.androidContext

class KmpBoilerplateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        bootstrap {
            androidContext(this@KmpBoilerplateApplication)
        }
    }
}
