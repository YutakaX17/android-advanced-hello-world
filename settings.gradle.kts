pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

includeBuild("../kmp-advanced-hello-world-core") {
    dependencySubstitution {
        substitute(module("io.github.yutakax17.advancedhelloworld:kmp-advanced-hello-world-core"))
            .using(project(":core"))
    }
}
includeBuild("../compose-advanced-hello-world-core") {
    dependencySubstitution {
        substitute(module("io.github.yutakax17.advancedhelloworld:compose-advanced-hello-world-core"))
            .using(project(":ui"))
    }
}
includeBuild("../kmp-advanced-hello-world-messages") {
    dependencySubstitution {
        substitute(module("io.github.yutakax17.advancedhelloworld:kmp-advanced-hello-world-messages"))
            .using(project(":messages"))
    }
}
includeBuild("../compose-advanced-hello-world-messages") {
    dependencySubstitution {
        substitute(module("io.github.yutakax17.advancedhelloworld:compose-advanced-hello-world-messages"))
            .using(project(":messages-ui"))
    }
}

rootProject.name = "android-advanced-hello-world"
include(":app")
