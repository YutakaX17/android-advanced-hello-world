package io.github.yutakax17.advancedhelloworld.android

import io.github.yutakax17.advancedhelloworld.compose.core.FeatureDestination
import io.github.yutakax17.advancedhelloworld.compose.core.FeatureUi
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesFeatureFactory
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesUiDependencies

public data class ModuleDescriptor(
    public val id: String,
    public val kind: String,
    public val coordinate: String,
    public val version: String,
)

public object GeneratedFeatureRegistry {
    public const val FEATURE_CONTRACT_VERSION: Int = 1
    public const val START_FEATURE_ID: String = "messages"

    public val modules: List<ModuleDescriptor> = listOf(
        ModuleDescriptor(
            id = "kmp-core",
            kind = "foundation",
            coordinate = "io.github.yutakax17.advancedhelloworld:kmp-advanced-hello-world-core",
            version = "0.1.0",
        ),
        ModuleDescriptor(
            id = "compose-core",
            kind = "foundation",
            coordinate = "io.github.yutakax17.advancedhelloworld:compose-advanced-hello-world-core",
            version = "0.1.0",
        ),
        ModuleDescriptor(
            id = "kmp-messages",
            kind = "domain",
            coordinate = "io.github.yutakax17.advancedhelloworld:kmp-advanced-hello-world-messages",
            version = "0.1.0",
        ),
        ModuleDescriptor(
            id = "messages",
            kind = "feature",
            coordinate = "io.github.yutakax17.advancedhelloworld:compose-advanced-hello-world-messages",
            version = "0.1.0",
        ),
    )

    public fun createFeatures(
        messagesDependencies: MessagesUiDependencies,
    ): List<FeatureUi> = listOf(
        MessagesFeatureFactory.create(messagesDependencies),
    )

    public fun startDestination(features: List<FeatureUi>): FeatureDestination {
        require(features.map(FeatureUi::id).distinct().size == features.size) {
            "feature ids must be unique"
        }
        val feature = features.singleOrNull { it.id == START_FEATURE_ID }
            ?: error("missing start feature: $START_FEATURE_ID")
        require(feature.contractVersion == FEATURE_CONTRACT_VERSION) {
            "incompatible feature UI contract"
        }
        return feature.destinations.firstOrNull()
            ?: error("start feature has no destinations: $START_FEATURE_ID")
    }
}
