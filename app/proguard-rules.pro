# Tink (via androidx.security-crypto) référence des annotations de compilation
# errorprone qui ne sont jamais présentes à l'exécution.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class net.mada.lumea.backup.** {
    *** Companion;
}
-keepclasseswithmembers class net.mada.lumea.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
