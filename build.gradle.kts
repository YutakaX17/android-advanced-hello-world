plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.dependency.check)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(
            mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable"),
        )
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
    }
    format("misc") {
        target("*.md", ".github/**/*.yml", ".github/**/*.yaml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    source.setFrom(files("app/src/main/kotlin"))
    config.setFrom(files("config/detekt/detekt.yml"))
}

dependencyCheck {
    failBuildOnCVSS = 7.0F
    formats = listOf("HTML", "SARIF")
    suppressionFile = "config/dependency-check-suppressions.xml"
    nvd.apiKey = providers.environmentVariable("NVD_API_KEY").orNull
    nvd.delay = 6_000
    nvd.resultsPerPage = 2_000
    nvd.maxRetryCount = 10
    nvd.validForHours = 4
}

dependencyAnalysis {
    issues {
        all {
            onAny { severity("fail") }
        }
    }
}
