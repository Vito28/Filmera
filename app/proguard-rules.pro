# Keep useful line information for release crash reports while allowing R8 to
# optimize and obfuscate implementation details.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp detects these optional security providers at runtime. They are not
# required when Android's platform TLS provider is used.
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
