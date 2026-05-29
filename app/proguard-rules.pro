# ServerControl ProGuard Rules

# ===== Kotlin =====
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# ===== Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ===== Hilt / Dagger =====
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}

# ===== Room =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ===== Retrofit / OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ===== Gson (used by Retrofit) =====
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep all DTO classes (they have @SerializedName annotations)
-keep class com.servercontrol.data.remote.dto.** { *; }

# ===== Domain Models (used in serialization) =====
-keep class com.servercontrol.domain.model.** { *; }

# ===== JSch (SSH library) =====
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# ===== WorkManager =====
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# ===== Glance Widgets =====
-keep class androidx.glance.** { *; }

# ===== ZXing (QR codes) =====
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ===== ML Kit (barcode scanning) =====
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ===== CameraX =====
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ===== Navigation =====
-keep class androidx.navigation.** { *; }

# ===== DataStore =====
-keep class androidx.datastore.** { *; }

# ===== Remove logging in release =====
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ===== Keep BuildConfig =====
-keep class com.servercontrol.BuildConfig { *; }

# ===== Enum classes =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
