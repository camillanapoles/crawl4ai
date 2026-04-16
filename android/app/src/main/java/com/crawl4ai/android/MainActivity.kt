package com.crawl4ai.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.crawl4ai.android.ui.navigation.Crawl4AINavHost
import com.crawl4ai.android.ui.theme.Crawl4AITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity hosting the entire Compose UI.
 *
 * Responsibilities:
 * - Enable edge-to-edge display (Android 15 requirement)
 * - Request POST_NOTIFICATIONS permission at runtime (Android 13+)
 * - Hand off to [Crawl4AINavHost] for all navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ── Runtime permission launcher ────────────────────────────────────────
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Notification permission result — the UI adapts gracefully
            // if the user denies (crawl still works, just no progress notification)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: removes status bar / nav bar background tinting
        enableEdgeToEdge()

        // Request notification permission (Android 13 / API 33+)
        requestNotificationPermissionIfNeeded()

        setContent {
            Crawl4AITheme {
                Crawl4AINavHost()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}
