# Firestore deserializes documents into these data classes via reflection.
# Keep their fields/no-arg constructors intact after R8 shrinking+obfuscation.
-keepclassmembers class com.tripro.app.data.model.** {
  *;
}
-keep class com.tripro.app.data.model.** { *; }
