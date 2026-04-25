# -------------------------------------------------------------------------
# VIETCORE OMNIS PRO - CORE SECURITY CONFIGURATION
# Project: com.example.myempty.vietcore
# Version: 26.1.2-Omnis Pro
# -------------------------------------------------------------------------

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# -------------------------------------------------------------------------
# 1. GLOBAL OBFUSCATION & OPTIMIZATION TIGHTENING
# -------------------------------------------------------------------------

# Optimization passes (Set to 5 for deep code analysis)
-optimizationpasses 5

# Allow access modification to make classes private/final where possible
-allowaccessmodification

# Flatten the package hierarchy to hide the original project structure
-flattenpackagehierarchy 'core.security.shield'
-repackageclasses 'core.security.logic'

# Rename all source files to hide original file names
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# -------------------------------------------------------------------------
# 2. KEEP RULES FOR CORE SYSTEM (DO NOT REMOVE)
# -------------------------------------------------------------------------

# Keep essential Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# -------------------------------------------------------------------------
# 3. SYNCHRONIZED SECURITY INTEGRITY (OMNIS SHIELD)
# -------------------------------------------------------------------------

# Keep our Security and Hardware classes but obfuscate internal methods
-keep class com.example.myempty.vietcore.OmnisSecurity { *; }
-keep class com.example.myempty.vietcore.DeviceSimulator { *; }
-keep class com.example.myempty.vietcore.SecurityClient { *; }

# Protect GSON models and Serialization from being renamed (Required for Network)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# -------------------------------------------------------------------------
# 4. NETWORK & OKHTTP SECURITY SYNC
# -------------------------------------------------------------------------

# Prevent OkHttp and Okio from being broken during obfuscation
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# -------------------------------------------------------------------------
# 5. ANTI-TAMPER & REFLECTION PROTECTION
# -------------------------------------------------------------------------

# Avoid keeping any unnecessary debugging information
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep the JavaScript interface if your project uses WebView (Fixed structure)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve Native method names for arm64-v8a stability
-keepclasseswithmembernames class * {
    native <methods>;
}

# -------------------------------------------------------------------------
# 6. FINAL SHIELD LOCKDOWN
# -------------------------------------------------------------------------

# Force ProGuard to treat every class as potentially modifiable
-overloadaggressively

# Ignore warnings from libraries to ensure successful build
-ignorewarnings

# -------------------------------------------------------------------------
# END OF CONFIGURATION - OMNIS PRO CORE ACTIVE
# -------------------------------------------------------------------------
