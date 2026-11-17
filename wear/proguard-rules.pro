# Wear OS ProGuard rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontobfuscate

# Keep serialization
-keepclassmembers class rigatoni534.apps.PastaPlayer.shared.** {
    *;
}
