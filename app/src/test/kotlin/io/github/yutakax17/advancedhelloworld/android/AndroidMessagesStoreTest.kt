package io.github.yutakax17.advancedhelloworld.android

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.yutakax17.advancedhelloworld.core.Clock
import io.github.yutakax17.advancedhelloworld.core.SyncResult
import io.github.yutakax17.advancedhelloworld.core.UuidGenerator
import io.github.yutakax17.advancedhelloworld.core.network.BackendConfiguration
import io.github.yutakax17.advancedhelloworld.core.network.createBackendHttpClient
import io.github.yutakax17.advancedhelloworld.messages.CreateMessageResult
import io.github.yutakax17.advancedhelloworld.messages.KtorMessageRemoteDataSource
import io.github.yutakax17.advancedhelloworld.messages.MessageSyncState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
@Config(sdk = [35], application = Application::class)
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

    @Test
    fun repositoryAndWorkerShareTheSameSynchronizationContributor() {
        createStore().use { store ->
            assertTrue(store.repository.syncContributor === store.syncContributor)
        }
    }

    @Test
    fun repeatedSynchronizationReusesIdempotencyKeyAndReconcilesMessage() = runBlocking {
        var postCount = 0
        val idempotencyKeys = mutableListOf<String>()
        val engine =
            MockEngine { request ->
                if (request.method.value == "POST") {
                    postCount += 1
                    idempotencyKeys += requireNotNull(request.headers[KtorMessageRemoteDataSource.IDEMPOTENCY_KEY_HEADER])
                    respond(
                        content = """{"id":"remote-1","text":"Persist me","createdAt":"2026-08-13T08:00:00Z"}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                } else {
                    respond(
                        content = """[{"id":"remote-1","text":"Persist me","createdAt":"2026-08-13T08:00:00Z"}]""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        val client =
            createBackendHttpClient(
                engine = engine,
                configuration = BackendConfiguration("https://example.test"),
            )

        createStore(httpClient = client).use { store ->
            assertTrue(store.repository.createOffline("Persist me") is CreateMessageResult.Created)

            assertEquals(SyncResult.Success, store.syncContributor.synchronize())
            assertEquals(SyncResult.Success, store.syncContributor.synchronize())

            val message = store.repository.listLocal().single()
            assertEquals(1, postCount)
            assertEquals(listOf("operation-id"), idempotencyKeys)
            assertEquals("remote-1", message.remoteId)
            assertEquals(MessageSyncState.SYNCED, message.syncState)
            assertEquals(0L, store.database.messagesQueries.countOutbox().executeAsOne())
        }
    }

    private fun createStore(
        httpClient: io.ktor.client.HttpClient? = null,
    ): AndroidMessagesStore {
        val ids = ArrayDeque(listOf("local-id", "operation-id", "pull-id-1", "pull-id-2"))
        return AndroidMessagesStore(
            context = context,
            databaseName = databaseName,
            queryDispatcher = Dispatchers.Unconfined,
            clock = Clock { 1_700_000_000_000L },
            uuidGenerator = UuidGenerator { ids.removeFirst() },
            httpClient = httpClient ?: defaultTestHttpClient(),
        )
    }

    private fun defaultTestHttpClient(): io.ktor.client.HttpClient = createBackendHttpClient(
        engine = MockEngine { error("unexpected backend request") },
        configuration = BackendConfiguration("https://example.test"),
    )
}
