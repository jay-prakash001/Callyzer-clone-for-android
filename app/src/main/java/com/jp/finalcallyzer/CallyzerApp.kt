package com.jp.finalcallyzer

import android.app.Activity
import android.app.Application
import androidx.work.Configuration
import com.google.android.datatransport.BuildConfig
import com.jp.finalcallyzer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CallyzerApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@CallyzerApp)
//            workManagerFactory()          // Koin WorkManager integration
            modules(appModule)
        }
    }


    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}