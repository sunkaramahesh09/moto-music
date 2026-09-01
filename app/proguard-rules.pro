# Moto Music keeps no reflection-driven code of its own; these rules cover the libraries
# that do, so the release build behaves exactly like the debug one.

# Room reads entity and DAO metadata generated at build time.
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(...); }

# Media3 resolves the session service and its callbacks from the manifest and by name.
-keep class androidx.media3.session.** { *; }
-keep class com.motomusic.app.playback.MusicService { *; }

# Kotlin coroutines' service loader entries are consulted at runtime.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep the enum-of-storage-keys mapping intact: values are persisted as strings.
-keepclassmembers enum com.motomusic.app.** { *; }
