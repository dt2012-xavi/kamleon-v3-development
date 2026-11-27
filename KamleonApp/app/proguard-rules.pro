# R8 rules for the app. Hilt, Room, Compose, Navigation, Firebase, ML Kit, CameraX and Mixpanel bring
# their own; these cover what only the app knows.

# Firestore maps these classes by reflection, reading (toObject/toObjects) AND writing (set) them: R8 must
# keep their names, fields, getters, setters and no-argument constructors, or a new account would be
# written to Firestore with renamed fields. Add here any other class passed to toObject() or set().
-keep class com.dynatech2012.kamleonuserapp.models.CustomUser { *; }
-keep class com.dynatech2012.kamleonuserapp.models.KamleonLegal { *; }
-keep class com.dynatech2012.kamleonuserapp.models.KamleonPermissions { *; }
-keep class com.dynatech2012.kamleonuserapp.models.RawMeasureData { *; }
# Their enums are written and read by constant name.
-keep enum com.dynatech2012.kamleonuserapp.models.Gender { *; }
-keep enum com.dynatech2012.kamleonuserapp.models.UserStatus { *; }
-keep enum com.dynatech2012.kamleonuserapp.models.MeasureType { *; }
-keep enum com.dynatech2012.kamleonuserapp.models.MeasurePrecision { *; }
# Generic field types (Map<String, KamleonPermissions>) and @PropertyName are needed at runtime.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Readable crash reports: Crashlytics deobfuscates with the mapping file the plugin uploads on release
# builds; it needs the line numbers. Source file names are hidden.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception
