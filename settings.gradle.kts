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
        listOf(
            "kmp-advanced-hello-world-core",
            "compose-advanced-hello-world-core",
            "kmp-advanced-hello-world-messages",
            "compose-advanced-hello-world-messages",
        ).forEach { repositoryName ->
            maven {
                name = "GitHubPackages-$repositoryName"
                url = uri("https://maven.pkg.github.com/YutakaX17/$repositoryName")
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orNull
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull
                }
                content {
                    includeGroup("io.github.yutakax17.advancedhelloworld")
                }
            }
        }
    }
}

val useLocalCompositeBuilds =
    providers
        .gradleProperty("useLocalCompositeBuilds")
        .map(String::toBoolean)
        .getOrElse(false)

if (useLocalCompositeBuilds) {
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
}

rootProject.name = "android-advanced-hello-world"
include(":app")
