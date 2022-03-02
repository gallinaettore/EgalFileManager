# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Ettore\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile




-keepattributes Signature
-keepattributes EnclosingMethod  #evita i warning InnerClasses


#importante non offuscare queste classi (vedi documentazione google: https://developer.android.com/google/play/billing/billing_library_overview#java)
-keep class com.android.vending.billing.**

#evita i warnings di jcifs
-keep class jcifs.** {*;}
-dontwarn jcifs.http.**

#evita i warning di firebase (se ad esempio importo la libreria firebase utils, ma nel mio gradle non usa ad esempio la remote config, necessario però alla liberia)
-dontwarn com.google.firebase.**

#lascio i nomi dei fragment per i miei log
-keepnames class * extends android.support.v4.app.Fragment 
-keepnames class * extends androidx.fragment.app.Fragment

# Senza questo i Companion usati da Java non verrebbero trovati (l'app crasha)
-keepclassmembers class ** {
    public static ** Companion;
}

#glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder


#non offusca i nomi delle classi esterne (sottopackages inclusi)
-keepnames class com.google.** {*;}
-keepnames class android.support.** {*;}
-keepnames class androidx.** {*;}

