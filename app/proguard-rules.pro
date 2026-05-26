# Add project specific ProGuard rules here.

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.example.okakapp.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.example.okakapp.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Markdown renderer
-dontwarn com.mikepenz.**

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
