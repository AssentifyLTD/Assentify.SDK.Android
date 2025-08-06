# Preserve all classes in the Assentify SDK package
-keep class com.assentify.sdk.** { *; }

# Tensorflow
-keep class org.tensorflow.** { *;}
-keep class org.tensorflow.lite.* { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# Keep models
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Scuba
-keep class net.sf.scuba.** { *; }
-dontwarn net.sf.scuba.**

# JMRTD
-keep class org.jmrtd.** { *; }
-dontwarn org.jmrtd.**

# Commons IO
-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.io.**

# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keepnames class org.bouncycastle.asn1.* { *; }

# JNBIS
-keep class com.github.mhshams.jnbis.** { *; }
-dontwarn com.github.mhshams.jnbis.**

# General rules for reflection and serialization
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Preserve all classes and methods in Clarity SDK
-keep class com.microsoft.clarity.** { *; }

# Preserve all models and their members (in case of reflection use)
-keepclassmembers class com.microsoft.clarity.** {
    *;
}

# Keep annotations (in case Clarity uses them internally)
-keepattributes *Annotation*

# Don't warn about missing references inside the SDK
-dontwarn com.microsoft.clarity.**