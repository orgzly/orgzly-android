# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
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


# Keep line numbers and file names
-keepattributes SourceFile,LineNumberTable

# Dropbox SDK Serialization

-keepattributes *Annotation*,EnclosingMethod,InnerClasses,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# Dropbox SSL trusted certs

-adaptresourcefilenames com/dropbox/core/http/trusted-certs.raw

# OkHttp and Servlet optional dependencies

-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-dontwarn javax.servlet.**
-dontwarn com.dropbox.core.http.GoogleAppEngineRequestor
-dontwarn com.dropbox.core.http.OkHttp3Requestor*
-dontwarn com.dropbox.core.http.GoogleAppEngineRequestor$Uploader
-dontwarn com.dropbox.core.http.GoogleAppEngineRequestor$FetchServiceUploader

# Support classes for compatibility with older API versions

-dontwarn android.support.**
-dontnote android.support.**

-dontwarn org.joda.convert.**

-dontwarn org.eclipse.jgit.**
-dontwarn com.jcraft.**
-dontwarn org.slf4j.**

-keepclassmembers enum com.orgzly.android.ui.refile.RefileLocation$Type { *; }