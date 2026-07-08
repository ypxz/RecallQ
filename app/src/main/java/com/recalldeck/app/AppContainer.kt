package com.recalldeck.app

import android.content.Context

/**
 * Manual dependency injection container. Repositories and other app-wide
 * singletons are created lazily here and handed to ViewModels.
 */
class AppContainer(private val applicationContext: Context)
