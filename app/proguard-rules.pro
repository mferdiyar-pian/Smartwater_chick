# ============================================================
# ProGuard/R8 Rules - SmartWaterChick
# ============================================================

# Optimization & Obfuscation settings
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses 'swc'

# Keep source attributes for debugging, but remove SourceFile names
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Keep only Firebase model class that needs JSON serialization/deserialization
-keep class com.example.smartwaterchick.JadwalItem {
    public <fields>;
    public <methods>;
}

# Keep enum methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable creator fields
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove Log calls in release builds to prevent leakage
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}