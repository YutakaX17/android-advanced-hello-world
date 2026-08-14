plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

val releaseApiBaseUrl = providers.gradleProperty("releaseApiBaseUrl").orElse("")
val debugApiBaseUrl = providers.gradleProperty("debugApiBaseUrl").orElse("http://10.0.2.2:8000")
val releaseKeystoreFile = providers.environmentVariable("ANDROID_KEYSTORE_FILE")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")

val validateReleaseApiBaseUrl =
    tasks.register<Exec>("validateReleaseApiBaseUrl") {
        group = "verification"
        description = "Requires a non-empty HTTPS backend URL for release builds."
        workingDir(rootDir)
        commandLine(
            "python3",
            "scripts/validate_release_endpoint.py",
            "--url",
            releaseApiBaseUrl.get(),
        )
    }

val verifyReleaseBuildConfig =
    tasks.register<Exec>("verifyReleaseBuildConfig") {
        group = "verification"
        description = "Verifies the exact HTTPS endpoint embedded in release BuildConfig."
        dependsOn("generateReleaseBuildConfig")
        workingDir(rootDir)
        commandLine(
            "python3",
            "scripts/verify_release_build_config.py",
            "--build-config",
            "app/build/generated/source/buildConfig/release/" +
                "io/github/yutakax17/advancedhelloworld/android/BuildConfig.java",
            "--expected-url",
            releaseApiBaseUrl.get(),
        )
    }

android {
    namespace = "io.github.yutakax17.advancedhelloworld.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yutakax17.advancedhelloworld"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystoreFile.isPresent) {
                storeFile = file(releaseKeystoreFile.get())
                storePassword = releaseKeystorePassword.orNull
                keyAlias = releaseKeyAlias.orNull
                keyPassword = releaseKeyPassword.orNull
            }
        }
    }

    buildTypes {
        debug {
            val escapedUrl =
                debugApiBaseUrl
                    .get()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
            buildConfigField("String", "API_BASE_URL", "\"$escapedUrl\"")
        }
        release {
            if (releaseKeystoreFile.isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val escapedUrl =
                releaseApiBaseUrl
                    .get()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
            buildConfigField("String", "API_BASE_URL", "\"$escapedUrl\"")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        dependsOn(validateReleaseApiBaseUrl)
    }
    if (name == "assembleRelease" || name == "lintRelease") {
        dependsOn(verifyReleaseBuildConfig)
    }
}

dependencies {
    implementation(libs.activity.compose)
    implementation(libs.advanced.hello.world.kmp.core)
    implementation(libs.advanced.hello.world.compose.core)
    implementation(libs.advanced.hello.world.kmp.messages)
    implementation(libs.advanced.hello.world.compose.messages)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.work.runtime)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.work.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.junit)
}

val checkModuleRegistry =
    tasks.register<Exec>("checkModuleRegistry") {
        group = "verification"
        description = "Validates modules.json and verifies that the generated registry is current."
        workingDir(rootDir)
        commandLine("python3", "scripts/generate_feature_registry.py", "--check")
    }

tasks.named("check") {
    dependsOn(checkModuleRegistry)
}
