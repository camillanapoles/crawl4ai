package com.crawl4ai.android.di

import android.content.Context
import com.crawl4ai.android.bridge.PythonBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-scoped singleton dependencies:
 * - [PythonBridge] — Chaquopy gateway to the Python crawl4ai runtime
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePythonBridge(
        @ApplicationContext context: Context,
    ): PythonBridge = PythonBridge(context)
}
