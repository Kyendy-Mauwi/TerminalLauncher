# TTY Launcher R8 rules.
# Room generated implementations are referenced reflectively in a few places.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Keep enum names used for persisted theme / cursor identifiers.
-keepclassmembers enum com.tty.launcher.domain.models.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Hilt / Dagger
-keep,allowobfuscation @interface dagger.hilt.** { *; }
