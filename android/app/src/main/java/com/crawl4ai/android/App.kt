package com.crawl4ai.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class.
 *
 * Responsibilities:
 * - Trigger Hilt code generation ([@HiltAndroidApp])
 * - Initialise the Chaquopy Python runtime
 * - Create notification channels required by the foreground service
 * - Configure WorkManager with Hilt-provided worker factory
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // ── Initialise Python runtime (Chaquopy) ──────────────────────────
        // Must be called exactly once before any Python() call.
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // ── Set CRAWL4AI_CACHE_DIR so Python code can write to the app's
        //    private files directory (no external storage permission needed)
        val cacheDir = filesDir.resolve("crawl4ai_cache")
        cacheDir.mkdirs()
        // Python engine reads this env var during init()
        System.setProperty("CRAWL4AI_CACHE_DIR", cacheDir.absolutePath)

        // ── Notification channels (required Android 8+) ───────────────────
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Channel for active crawl foreground service
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CRAWL,
                getString(R.string.notif_channel_crawl_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notif_channel_crawl_desc)
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_CRAWL = "crawl4ai_crawl"
    }
}
