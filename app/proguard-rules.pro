# Proguard rules for Macro Gaming app
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.macrophone.gaming.data.model.** { *; }
