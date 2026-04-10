##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# The JNA library references Java AWT classes which are not part of the Android SDK.
# Tell R8 not to warn about them, as this code path is not used on Android.
-dontwarn java.awt.**

# Gson specific classes
#-keep class com.google.gson.stream.** { *; }
-keep class com.dzungphung.aimodel.econimical.smartspend.di.** { <fields>; }
-keep class com.dzungphung.aimodel.econimical.smartspend.data.** { <fields>; }
-keep class com.dzungphung.aimodel.econimical.smartspend.ui.** { <fields>; }
-keep class com.dzungphung.aimodel.econimical.smartspend.util.** { <fields>; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.mlkit.vision.** { *; }
# Application classes that will be serialized/deserialized over Gson
# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.

##---------------End: proguard configuration for Gson  ----------