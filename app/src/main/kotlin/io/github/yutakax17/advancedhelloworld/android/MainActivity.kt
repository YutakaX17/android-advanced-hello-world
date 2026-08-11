package io.github.yutakax17.advancedhelloworld.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.yutakax17.advancedhelloworld.compose.core.AppShell
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesStateHolder
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesUiDependencies

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = (application as AdvancedHelloWorldApplication).messagesInteractor
        setContent {
            val scope = rememberCoroutineScope()
            val stateHolder = remember(controller, scope) {
                MessagesStateHolder(controller, scope)
            }
            val features = GeneratedFeatureRegistry.createFeatures(
                messagesDependencies = MessagesUiDependencies(stateHolder),
            )
            val startDestination = GeneratedFeatureRegistry.startDestination(features)
            AppShell {
                startDestination.content()
            }
        }
    }
}
