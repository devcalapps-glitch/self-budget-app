# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Gson reflectively reads/writes the data model package's field names when serializing backup
# exports (SyncDataPayload + every Room entity inside it, see CloudSyncManager.kt) - keep the
# classes, fields, and enum values intact so field renaming/removal by R8 can't corrupt a
# restored backup or silently drop data.
-keep class com.selfbudget.app.data.model.** { *; }
-keepclassmembers enum com.selfbudget.app.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes Signature
-keepattributes *Annotation*

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
