package io.github.yutakax17.advancedhelloworld.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.yutakax17.advancedhelloworld.compose.core.AppShell
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesFeatureFactory
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesInteractor
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesStateHolder
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesUiDependencies
import io.github.yutakax17.advancedhelloworld.core.SyncResult
import io.github.yutakax17.advancedhelloworld.messages.CreateMessageResult
import io.github.yutakax17.advancedhelloworld.messages.Message
import io.github.yutakax17.advancedhelloworld.messages.MessageSyncState
import io.github.yutakax17.advancedhelloworld.messages.MessageValidation
import io.github.yutakax17.advancedhelloworld.messages.validateMessageText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = PreviewMessagesController()
        setContent {
            val scope = rememberCoroutineScope()
            val stateHolder = remember(controller, scope) {
                MessagesStateHolder(controller, scope)
            }
            val feature = MessagesFeatureFactory.create(
                MessagesUiDependencies(stateHolder),
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
private class PreviewMessagesController : MessagesInteractor {
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    override fun observeMessages(): Flow<List<Message>> = messages.asStateFlow()

    override suspend fun createMessage(text: String): CreateMessageResult {
        val validation = validateMessageText(text)
        if (validation !is MessageValidation.Valid) {
            return CreateMessageResult.Rejected(validation)
        }
        val message = Message(
            localId = UUID.randomUUID().toString(),
            remoteId = null,
            text = validation.normalizedText,
            createdAtLocal = System.currentTimeMillis(),
            createdAtServer = null,
            syncState = MessageSyncState.PENDING,
        )
        messages.update { current -> listOf(message) + current }
        return CreateMessageResult.Created(message)
    }

    override suspend fun refresh(): SyncResult = SyncResult.Retry("Backend retrieval is not wired yet.")

    override suspend fun retry(localId: String): SyncResult {
        return SyncResult.Retry("Synchronization retry is not wired yet for $localId.")
    }
}
