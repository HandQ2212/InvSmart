import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.invsmart.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.invsmart.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        val url = properties.getProperty("SUPABASE_URL") ?: ""
        val apiKey = properties.getProperty("SUPABASE_KEY") ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"$url\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")

    val supabase_version = "2.1.0"
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabase_version")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabase_version")
    implementation("io.github.jan-tennert.supabase:realtime-kt:$supabase_version")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabase_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    val camerax_version = "1.4.2"
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
}

kapt {
    correctErrorTypes = true
}