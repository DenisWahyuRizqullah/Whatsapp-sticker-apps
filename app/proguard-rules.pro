# Keep FFmpegKit classes and native JNI callbacks for R8 release builds
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**
