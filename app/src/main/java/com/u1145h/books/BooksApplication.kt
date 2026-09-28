package com.u1145h.books

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Enables Hilt, registers the auth-aware Coil
 * [ImageLoader] globally, and configures WorkManager to use Hilt's
 * [HiltWorkerFactory] so workers can receive injected dependencies.
 */
@HiltAndroidApp
class BooksApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        coil.Coil.setImageLoader(imageLoader)
    }

    override fun newImageLoader(): ImageLoader = imageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
