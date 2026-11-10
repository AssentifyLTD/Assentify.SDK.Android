########################################
# General
########################################
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

########################################
# Assentify SDK
########################################
-keep class com.assentify.sdk.** { *; }

########################################
# TensorFlow / LiteRT
########################################
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

########################################
# OpenCV (org.opencv:opencv)
# The Java classes are bound to the native library (libopencv_java*.so) by their exact
# names through JNI: if R8 renames or removes them, release builds crash at runtime.
########################################
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# any class with native methods (OpenCV's, and other JNI classes) keeps its names
-keepclasseswithmembernames class * {
    native <methods>;
}

########################################
# Retrofit / Gson
########################################
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

########################################
# NFC: Scuba, JMRTD, Commons IO, BouncyCastle, JNBIS, JP2
# (JMRTD and BouncyCastle use reflection; stripping them breaks NFC only in release builds)
########################################
-keep class net.sf.scuba.** { *; }
-dontwarn net.sf.scuba.**

-keep class org.jmrtd.** { *; }
-dontwarn org.jmrtd.**

-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.io.**

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# JNBIS real package is org.jnbis (the old rule used a package that does not exist)
-keep class org.jnbis.** { *; }
-dontwarn org.jnbis.**

-keep class dev.keiji.jp2.** { *; }
-dontwarn dev.keiji.jp2.**

########################################
# Microsoft Clarity
########################################
-keep class com.microsoft.clarity.** { *; }
-dontwarn com.microsoft.clarity.**

########################################
# Bugsnag
########################################
-keep class com.bugsnag.** { *; }
-keepclassmembers class * {
    public void onError(com.bugsnag.android.Event);
}
-keepclassmembers class * {
    @com.bugsnag.android.BreadcrumbType <fields>;
}