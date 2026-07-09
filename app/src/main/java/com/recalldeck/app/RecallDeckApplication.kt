package com.recalldeck.app

import android.app.Application

/**
 * Application entry point. Holds the manual DI container.
 */
class RecallDeckApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
