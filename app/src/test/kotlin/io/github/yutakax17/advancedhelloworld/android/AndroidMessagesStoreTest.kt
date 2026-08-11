package io.github.yutakax17.advancedhelloworld.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.yutakax17.advancedhelloworld.core.Clock
import io.github.yutakax17.advancedhelloworld.core.UuidGenerator
import io.github.yutakax17.advancedhelloworld.messages.CreateMessageResult
import io.github.yutakax17.advancedhelloworld.messages.MessageSyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidMessagesStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "messages-store-test.db"

    @Before
    fun deleteDatabaseBeforeTest() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun deleteDatabaseAfterTest() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun messageAndPendingOutboxSurviveStoreRecreation() = runBlocking {
        createStore().use { firstStore ->
            val result = firstStore.repository.createOffline("Persist me")
            assertTrue(result is CreateMessageResult.Created)
            assertEquals(1L, firstStore.database.messagesQueries.countOutbox().executeAsOne())
        }

        createStore().use { recreatedStore ->
            val persistedMessages = recreatedStore.repository.listLocal()

            assertEquals(1, persistedMessages.size)
            assertEquals("Persist me", persistedMessages.single().text)
            assertEquals(MessageSyncState.PENDING, persistedMessages.single().syncState)
            assertEquals(1L, recreatedStore.database.messagesQueries.countOutbox().executeAsOne())
        }
    }

    private fun createStore(): AndroidMessagesStore {
        val ids = ArrayDeque(listOf("local-id", "operation-id"))
        return AndroidMessagesStore(
            context = context,
            databaseName = databaseName,
            queryDispatcher = Dispatchers.Unconfined,
            clock = Clock { 1_700_000_000_000L },
            uuidGenerator = UuidGenerator { ids.removeFirst() },
        )
    }
}
