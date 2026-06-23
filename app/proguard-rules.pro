# proj4j / NGA geopackage / sqlite : conserver (réflexion, registres de projection, JNI).
-keep class org.locationtech.proj4j.** { *; }
-keep class mil.nga.** { *; }
-keep class org.sqlite.** { *; }
-keep class org.osmdroid.** { *; }
-dontwarn org.locationtech.proj4j.**
-dontwarn mil.nga.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
# ormlite référence javax.persistence en option (absent sur Android).
-dontwarn javax.persistence.**
-dontwarn java.beans.**
-dontwarn org.json.**

# GeoPackage NGA s'appuie sur ORMLite (réflexion sur les DAO annotés) : tout conserver.
-keep class com.j256.ormlite.** { *; }
-keepclassmembers class mil.nga.** { *; }
-keep class org.locationtech.proj4j.** { *; }
-dontwarn com.j256.ormlite.**
