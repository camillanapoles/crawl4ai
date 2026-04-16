package com.crawl4ai.android.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "crawl4ai_prefs",
)

/**
 * Type-safe accessor for all Crawl4AI application preferences (DataStore).
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    // ── Keys ───────────────────────────────────────────────────────────────

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")              // "system" | "light" | "dark"
        val KEY_HISTORY_LIMIT = intPreferencesKey("history_limit") // max entries to keep
        val KEY_CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        val KEY_DEFAULT_TIMEOUT = intPreferencesKey("default_timeout_ms")
        val KEY_DEFAULT_WORD_THRESHOLD = intPreferencesKey("default_word_threshold")
        val KEY_AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
        val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val KEY_DOCKER_HOST = stringPreferencesKey("docker_host")
        val KEY_DOCKER_PORT = intPreferencesKey("docker_port")
        val KEY_DOCKER_TOKEN = stringPreferencesKey("docker_token")
        val KEY_USE_REMOTE_DOCKER = booleanPreferencesKey("use_remote_docker")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    // ── Typed accessors ────────────────────────────────────────────────────

    val theme: Flow<String> = store.data.map { it[KEY_THEME] ?: "system" }
    val historyLimit: Flow<Int> = store.data.map { it[KEY_HISTORY_LIMIT] ?: 200 }
    val cacheEnabled: Flow<Boolean> = store.data.map { it[KEY_CACHE_ENABLED] ?: true }
    val defaultTimeoutMs: Flow<Int> = store.data.map { it[KEY_DEFAULT_TIMEOUT] ?: 30_000 }
    val defaultWordThreshold: Flow<Int> = store.data.map { it[KEY_DEFAULT_WORD_THRESHOLD] ?: 10 }
    val autoClearCache: Flow<Boolean> = store.data.map { it[KEY_AUTO_CLEAR_CACHE] ?: false }
    val showNotifications: Flow<Boolean> = store.data.map { it[KEY_SHOW_NOTIFICATIONS] ?: true }
    val dockerHost: Flow<String> = store.data.map { it[KEY_DOCKER_HOST] ?: "" }
    val dockerPort: Flow<Int> = store.data.map { it[KEY_DOCKER_PORT] ?: 11235 }
    val dockerToken: Flow<String> = store.data.map { it[KEY_DOCKER_TOKEN] ?: "" }
    val useRemoteDocker: Flow<Boolean> = store.data.map { it[KEY_USE_REMOTE_DOCKER] ?: false }
    val onboardingDone: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    // ── Setters ────────────────────────────────────────────────────────────

    suspend fun setTheme(value: String) = store.edit { it[KEY_THEME] = value }
    suspend fun setHistoryLimit(value: Int) = store.edit { it[KEY_HISTORY_LIMIT] = value }
    suspend fun setCacheEnabled(value: Boolean) = store.edit { it[KEY_CACHE_ENABLED] = value }
    suspend fun setDefaultTimeoutMs(value: Int) = store.edit { it[KEY_DEFAULT_TIMEOUT] = value }
    suspend fun setDefaultWordThreshold(value: Int) = store.edit { it[KEY_DEFAULT_WORD_THRESHOLD] = value }
    suspend fun setAutoClearCache(value: Boolean) = store.edit { it[KEY_AUTO_CLEAR_CACHE] = value }
    suspend fun setShowNotifications(value: Boolean) = store.edit { it[KEY_SHOW_NOTIFICATIONS] = value }
    suspend fun setDockerHost(value: String) = store.edit { it[KEY_DOCKER_HOST] = value }
    suspend fun setDockerPort(value: Int) = store.edit { it[KEY_DOCKER_PORT] = value }
    suspend fun setDockerToken(value: String) = store.edit { it[KEY_DOCKER_TOKEN] = value }
    suspend fun setUseRemoteDocker(value: Boolean) = store.edit { it[KEY_USE_REMOTE_DOCKER] = value }
    suspend fun setOnboardingDone(value: Boolean) = store.edit { it[KEY_ONBOARDING_DONE] = value }
}
