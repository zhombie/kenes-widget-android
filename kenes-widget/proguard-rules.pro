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

# WebRTC
-keep class org.webrtc.** { *; }

-keepattributes InnerClasses

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class q19.kenes_widget.KenesWidget { *; }
-keep class q19.kenes_widget.KenesWidget { <methods>; }
-keep class q19.kenes_widget.KenesWidget$* { *; }
-keepnames class q19.kenes_widget.ui.presentation.KenesWidgetV2Activity
-keep class q19.kenes_widget.core.file.KenesWidgetFileProvider
