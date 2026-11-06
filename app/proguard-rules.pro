# Keep kotlinx.serialization generated serializers for the session model.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class de.tempotap.app.data.SessionRecord {
    *** Companion;
}
-keepclasseswithmembers class de.tempotap.app.data.SessionRecord {
    kotlinx.serialization.KSerializer serializer(...);
}
