package com.crawl4ai.android.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.crawl4ai.android.App
import com.crawl4ai.android.MainActivity
import com.crawl4ai.android.R
import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.usecase.CrawlSingleUrlUseCase
import com.crawl4ai.android.domain.usecase.DeepCrawlUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CrawlForegroundService
 * ────────────────────────
 * Runs long crawl operations in the foreground so Android does not kill
 * the process during network I/O.
 *
 * Android 14+ requires [ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC]
 * declared in the manifest (done) and passed to startForeground().
 *
 * Commands (via Intent actions):
 *  ACTION_START_CRAWL       — start a single URL crawl
 *  ACTION_START_DEEP_CRAWL  — start a deep crawl session
 *  ACTION_CANCEL            — cancel the running crawl
 */
@AndroidEntryPoint
class CrawlForegroundService : LifecycleService() {

    @Inject lateinit var crawlSingleUrlUseCase: CrawlSingleUrlUseCase
    @Inject lateinit var deepCrawlUseCase: DeepCrawlUseCase
    @Inject lateinit var eventBus: EventBus

    private var crawlJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_CRAWL -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: url
                startForegroundCompat(buildNotification("Crawling…", url))
                startSingleCrawl(jobId, url)
            }

            ACTION_START_DEEP_CRAWL -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: url
                startForegroundCompat(buildNotification("Deep crawl starting…", url))
                startDeepCrawl(jobId, url)
            }

            ACTION_CANCEL -> {
                crawlJob?.cancel()
                eventBus.emit(CrawlEvent.CrawlCancelled(
                    intent.getStringExtra(EXTRA_JOB_ID) ?: ""
                ))
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    // ── Crawl operations ───────────────────────────────────────────────────

    private fun startSingleCrawl(jobId: String, url: String) {
        crawlJob = lifecycleScope.launch {
            eventBus.emit(CrawlEvent.CrawlStarted(jobId, url))
            val start = System.currentTimeMillis()

            crawlSingleUrlUseCase(url, CrawlConfig())
                .onSuccess { response ->
                    updateNotification("Completed: $url")
                    eventBus.emit(
                        CrawlEvent.CrawlCompleted(
                            jobId = jobId,
                            url = url,
                            resultId = 0L, // repository sets actual ID
                            success = response.success,
                            durationMs = System.currentTimeMillis() - start,
                        )
                    )
                }
                .onFailure { error ->
                    eventBus.emit(CrawlEvent.CrawlFailed(jobId, url, error.message ?: ""))
                }

            stopSelf()
        }
    }

    private fun startDeepCrawl(jobId: String, startUrl: String) {
        crawlJob = lifecycleScope.launch {
            var pagesVisited = 0
            val start = System.currentTimeMillis()

            deepCrawlUseCase(startUrl, DeepCrawlConfig(), CrawlConfig()).collect { progress ->
                pagesVisited = progress.pagesVisited
                updateNotification(
                    "Deep crawl: $pagesVisited/${progress.maxPages} pages — ${progress.currentUrl}"
                )
                eventBus.emit(
                    CrawlEvent.DeepCrawlPageCrawled(
                        sessionId = jobId,
                        url = progress.currentUrl,
                        depth = progress.currentDepth,
                        pagesVisited = progress.pagesVisited,
                        pagesRemaining = progress.pagesRemaining,
                    )
                )
            }

            eventBus.emit(
                CrawlEvent.DeepCrawlCompleted(
                    sessionId = jobId,
                    totalPages = pagesVisited,
                    durationMs = System.currentTimeMillis() - start,
                )
            )
            stopSelf()
        }
    }

    // ── Notification helpers ───────────────────────────────────────────────

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification("Crawl4AI", text))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, CrawlForegroundService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, App.CHANNEL_CRAWL)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, getString(R.string.action_cancel), cancelIntent)
            .build()
    }

    companion object {
        const val ACTION_START_CRAWL = "com.crawl4ai.android.START_CRAWL"
        const val ACTION_START_DEEP_CRAWL = "com.crawl4ai.android.START_DEEP_CRAWL"
        const val ACTION_CANCEL = "com.crawl4ai.android.CANCEL"

        const val EXTRA_URL = "url"
        const val EXTRA_JOB_ID = "job_id"

        private const val NOTIFICATION_ID = 1001
    }
}
