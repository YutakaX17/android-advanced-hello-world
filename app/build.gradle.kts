plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

val releaseApiBaseUrl = providers.gradleProperty("releaseApiBaseUrl").orElse("")

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

android {
    namespace = "io.github.yutakax17.advancedhelloworld.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yutakax17.advancedhelloworld"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
        release {
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
