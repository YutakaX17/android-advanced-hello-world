package io.github.yutakax17.advancedhelloworld.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.yutakax17.advancedhelloworld.compose.core.AppShell
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesActions
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesFeatureFactory
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesState
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesUiDependencies
import io.github.yutakax17.advancedhelloworld.messages.Message
import io.github.yutakax17.advancedhelloworld.messages.MessageSyncState
import io.github.yutakax17.advancedhelloworld.messages.MessageValidation
import io.github.yutakax17.advancedhelloworld.messages.validateMessageText
import java.util.UUID

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        val controller = PreviewMessagesController()
        setContent {
            val feature = MessagesFeatureFactory.create(
                MessagesUiDependencies(controller.state, controller),
            )
            AppShell {
                feature.destinations.single().content()
            }
        }
    }
}

/**
 * Temporary assembler adapter. Durable SQLDelight storage and backend synchronization
 * replace this adapter in the next feature slice without changing the feature UI contract.
 */
private class PreviewMessagesController : MessagesActions {
    var state: MessagesState by mutableStateOf(MessagesState())
        private set

    override fun updateDraft(text: String): Unit {
        state = state.copy(draftText = text, userMessage = null)
    }

    override fun submit(): Unit {
        when (val validation = validateMessageText(state.draftText)) {
            MessageValidation.Blank -> state = state.copy(userMessage = "Enter a message.")
            is MessageValidation.TooLong ->
                state = state.copy(userMessage = "Messages can contain at most ${validation.maximumLength} characters.")
            is MessageValidation.Valid -> {
                val message = Message(
                    localId = UUID.randomUUID().toString(),
                    remoteId = null,
                    text = validation.normalizedText,
                    createdAtLocal = System.currentTimeMillis(),
                    createdAtServer = null,
                    syncState = MessageSyncState.PENDING,
                )
                state = state.copy(
                    messages = listOf(message) + state.messages,
                    draftText = "",
                    userMessage = "Saved on this device; synchronization is not wired yet.",
                )
            }
        }
    }

    override fun refresh(): Unit {
        state = state.copy(userMessage = "Backend retrieval is not wired yet.")
    }

    override fun retry(localId: String): Unit {
        state = state.copy(userMessage = "Synchronization retry is not wired yet.")
    }
}
