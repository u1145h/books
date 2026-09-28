# Readium
-keep class org.readium.** { *; }
-dontwarn org.readium.**
-keepattributes *Annotation*

# junrar
-dontwarn com.github.junrar.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx-serialization
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, *Annotation*
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.u1145h.books.data.remote.dto.** { *; }
-keep,includedescriptorclasses class com.u1145h.books.domain.model.** { *; }
-keep,includedescriptorclasses class com.u1145h.books.**$$serializer { *; }
-keepclassmembers class com.u1145h.books.** {
    *** Companion;
}
-keepclasseswithmembers class com.u1145h.books.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WebView JS interface
-keepclassmembers class com.u1145h.books.feature.home.KavitaBridge {
    public *;
}

# Keep Parcelable / Serializable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}
