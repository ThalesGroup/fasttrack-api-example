# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# There is an issue on R8 on GSON. Please refer to: https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md
# option 1
-keepclassmembers,allowobfuscation class util.a.y.** {
  <fields>;
}

# option 2
#-dontshrink
