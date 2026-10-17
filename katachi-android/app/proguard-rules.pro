# JNI entry points into liblumecore.so
-keep class app.katachi.LumeCore { native <methods>; }
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class app.katachi.core.**$$serializer { *; }
-keepclassmembers class app.katachi.core.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
