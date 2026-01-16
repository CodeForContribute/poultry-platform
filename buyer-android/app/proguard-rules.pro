# ============================================================================
# ProGuard Rules for Poultry Buyer Android App
# Production security hardening configuration
# ============================================================================

# ============================================================================
# GENERAL OPTIMIZATIONS
# ============================================================================

# Optimize code
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# ANDROID FRAMEWORK
# ============================================================================

# Keep all Android framework classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

# Keep Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# JETPACK COMPOSE
# ============================================================================

-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }

# ============================================================================
# HILT / DAGGER
# ============================================================================

-keepclasseswithmembernames class * {
    @dagger.* <methods>;
}

-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Factory { *; }
-keep class **_Factory$* { *; }
-keep class **_MembersInjector { *; }

# Keep @Inject annotated classes
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}

# ============================================================================
# RETROFIT / OKHTTP / NETWORKING
# ============================================================================

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Keep OkHttp platform classes
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================================
# GSON
# ============================================================================

-keepattributes Signature
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep generic type info for Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# API MODELS - Keep all data classes used for API communication
# ============================================================================

# Keep all API model classes
-keep class com.poultry.buyer.domain.model.** { *; }
-keepclassmembers class com.poultry.buyer.domain.model.** { *; }

# Keep all request/response classes
-keep class com.poultry.buyer.data.remote.dto.** { *; }
-keepclassmembers class com.poultry.buyer.data.remote.dto.** { *; }

# ============================================================================
# KOTLIN
# ============================================================================

-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Kotlin data classes
-keepclassmembers class ** {
    @kotlin.Metadata <methods>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.poultry.buyer.**$$serializer { *; }
-keepclassmembers class com.poultry.buyer.** {
    *** Companion;
}
-keepclasseswithmembers class com.poultry.buyer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# ROOM DATABASE
# ============================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ============================================================================
# COIL IMAGE LOADING
# ============================================================================

-dontwarn coil.**
-keep class coil.** { *; }

# ============================================================================
# RAZORPAY
# ============================================================================

-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ============================================================================
# SECURITY - CRITICAL RULES
# ============================================================================

# Keep SecurityManager for security checks
-keep class com.poultry.buyer.core.security.SecurityManager { *; }
-keep class com.poultry.buyer.core.security.SecurityManager$* { *; }

# Keep encryption/security classes
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Obfuscate security-related method names aggressively
# but keep the classes themselves
-keepclassmembernames class com.poultry.buyer.core.security.** {
    !public <methods>;
}

# Anti-tampering: Make reverse engineering harder
-repackageclasses 'p'
-allowaccessmodification
-flattenpackagehierarchy 'p'

# ============================================================================
# NATIVE CODE PROTECTION
# ============================================================================

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ============================================================================
# ENUMS
# ============================================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# R8 SPECIFIC RULES (Android R8 compiler)
# ============================================================================

# Keep annotation classes
-keep class * extends java.lang.annotation.Annotation { *; }

# Keep generic signatures for type safety
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ============================================================================
# REMOVE DEBUG CODE
# ============================================================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Remove Timber logging if used
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove println statements
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ============================================================================
# DATASTORE
# ============================================================================

-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============================================================================
# COROUTINES
# ============================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================================================
# LIFECYCLE / VIEWMODEL
# ============================================================================

-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
