// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Đồng bộ phiên bản Hilt 2.54 như bản chạy ổn
    id("com.google.dagger.hilt.android") version "2.54" apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    repositories {
        google()
    }
    dependencies {
        // Sử dụng classpath từ catalog để hỗ trợ SafeArgs XML
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
    }
}