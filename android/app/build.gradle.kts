// ──────────────────────────────────────────────────────────────────────────
//  Crawl4AI Android — App Module Build Script
//  Android 15 (API 35) | Chaquopy Python 3.10
// ──────────────────────────────────────────────────────────────────────────
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.chaquopy)
}

// ── Read optional local overrides ─────────────────────────────────────────
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.reader())
}

android {
    namespace = "com.crawl4ai.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.crawl4ai.android"
        minSdk = 28          // Android 9+ — broad coverage
        targetSdk = 35       // Android 15
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject optional server config from local.properties into BuildConfig
        buildConfigField(
            "String",
            "DOCKER_HOST",
            "\"${localProps.getProperty("crawl4ai.docker.host", "")}\""
        )
        buildConfigField(
            "String",
            "DOCKER_PORT",
            "\"${localProps.getProperty("crawl4ai.docker.port", "11235")}\""
        )
        buildConfigField(
            "String",
            "DOCKER_TOKEN",
            "\"${localProps.getProperty("crawl4ai.docker.token", "")}\""
        )

        // ABI split: include 64-bit ARM (most phones) + 32-bit ARM + x86_64 (emulators)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        // Room schema export directory
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    // ── Chaquopy Python 3.10 ──────────────────────────────────────────────
    chaquopy {
        defaultConfig {
            version = "3.10"

            // ─ Core HTTP + HTML parsing (run on-device, no browser required)
            pip {
                // HTTP clients
                install("aiohttp>=3.11.11")
                install("aiohttp[speedups]")
                install("httpx[http2]>=0.27.2")
                install("brotli>=1.1.0")

                // HTML/XML parsing
                install("lxml>=5.3.0")
                install("beautifulsoup4>=4.12.0")
                install("cssselect>=1.2.0")
                install("chardet>=5.2.0")

                // Data validation & serialization
                install("pydantic>=2.10.0")

                // Math / numerics
                install("numpy>=1.26.0")

                // Text relevance ranking
                install("rank-bm25>=0.2.2")

                // Async I/O utilities
                install("aiosqlite>=0.20.0")
                install("aiofiles>=24.1.0")
                install("anyio>=4.0.0")

                // NLP tokenisation (NlpSentenceChunking)
                install("nltk>=3.9.1")
                install("snowballstemmer>=2.2.0")

                // Fast hashing (cache fingerprinting)
                install("xxhash>=3.4.0")

                // Config / environment
                install("PyYAML>=6.0")
                install("python-dotenv>=1.0.0")

                // User-agent generation (anti-detection)
                install("fake-useragent>=2.2.0")

                // Colour output (CLI utilities imported by crawl4ai)
                install("colorama>=0.4.6")

                // Hashing utilities used by crawl4ai internals
                install("lark>=1.2.2")
            }
        }

        sourceSets {
            getByName("main") {
                srcDir("src/main/python")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/NOTICE.md",
                "/META-INF/LICENSE.md"
            )
        }
    }

    // Export Room schemas for migration tracking
    sourceSets {
        getByName("main") {
            assets.srcDirs("schemas")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // ── Kotlin stdlib ──────────────────────────────────────────────────────
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)

    // ── AndroidX Core ──────────────────────────────────────────────────────
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // ── Compose ────────────────────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.navigation.compose)

    // ── Hilt DI ────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Room ───────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── DataStore ──────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── WorkManager ────────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ── Networking (remote Docker client) ─────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // ── Image loading ──────────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Markdown rendering ─────────────────────────────────────────────────
    implementation(libs.markdown.compose)

    // ── Debug tooling ─────────────────────────────────────────────────────
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── Unit tests ────────────────────────────────────────────────────────
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.room.testing)
    testImplementation(libs.work.testing)

    // ── Instrumented tests ────────────────────────────────────────────────
    androidTestImplementation(libs.junit.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.coroutines.test)
}
