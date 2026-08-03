plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "io.github.yutakax17.advancedhelloworld.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yutakax17.advancedhelloworld"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.activity.compose)
    implementation(libs.advanced.hello.world.kmp.core)
    implementation(libs.advanced.hello.world.compose.core)
    implementation(libs.advanced.hello.world.kmp.messages)
    implementation(libs.advanced.hello.world.compose.messages)
}
