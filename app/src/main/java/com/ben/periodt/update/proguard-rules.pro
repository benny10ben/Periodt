# Keep key attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Entry points (manifest-referenced)
-keep class ** extends android.app.Activity
-keep class ** extends android.app.Service
-keep class ** extends android.content.BroadcastReceiver
-keep class ** extends android.appwidget.AppWidgetProvider

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# Jetpack Compose
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.** { *; }
-keep class androidx.activity.compose.** { *; }
-keep class androidx.lifecycle.viewmodel.compose.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep interface * implements androidx.room.Dao { *; }
-dontwarn androidx.room.paging.**

# SQLCipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }

# AndroidX Security
-keep class androidx.security.crypto.** { *; }

# Strip debug logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Gson — only keep the library itself, data classes are protected
# via @Keep annotations directly in source (BackupData.kt)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**