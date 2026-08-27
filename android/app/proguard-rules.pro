# Retained appliance rules if minification is re-enabled.
-keep class com.example.ava.microwakeword.** { *; }
-keep class com.example.microfeatures.** { *; }
-keep class com.example.ava.receivers.** { *; }
-keep class com.example.ava.services.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * implements com.google.protobuf.MessageLite { *; }
