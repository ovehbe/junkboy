# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }

# Room Database (works with both KAPT and KSP)
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep entity classes and DAOs
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# KSP Generated code
-keep class **.*_Impl { *; }
-keep class **.*_Factory { *; } 