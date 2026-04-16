# ProGuard / R8 rules — Crawl4AI Android
# These rules apply to RELEASE builds.

# ── Chaquopy (Python runtime) ─────────────────────────────────────────────
-keep class com.chaquo.python.** { *; }
-keep interface com.chaquo.python.** { *; }
-keepclassmembers class * {
    @com.chaquo.python.PyMethod *;
}
-dontwarn com.chaquo.python.**

# ── Kotlin ────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ── Kotlinx Serialization ─────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.crawl4ai.android.**$$serializer { *; }
-keepclassmembers class com.crawl4ai.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.crawl4ai.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Hilt ──────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers @dagger.hilt.android.AndroidEntryPoint class * { *; }
-dontwarn dagger.**
-dontwarn javax.inject.**

# ── Room ──────────────────────────────────────────────────────────────────
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Retrofit / OkHttp ─────────────────────────────────────────────────────
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# ── Gson ──────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Crawl4AI domain models ────────────────────────────────────────────────
# Keep all domain and bridge classes because they are referenced from Python
-keep class com.crawl4ai.android.domain.model.** { *; }
-keep class com.crawl4ai.android.bridge.** { *; }
-keep class com.crawl4ai.android.data.local.db.** { *; }

# ── WorkManager ───────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Coil ──────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Miscellaneous warnings to suppress ───────────────────────────────────
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
