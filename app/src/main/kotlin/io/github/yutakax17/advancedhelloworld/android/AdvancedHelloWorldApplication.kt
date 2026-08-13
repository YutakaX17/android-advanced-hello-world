package io.github.yutakax17.advancedhelloworld.android

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.yutakax17.advancedhelloworld.compose.messages.MessagesInteractor
import io.github.yutakax17.advancedhelloworld.compose.messages.RepositoryMessagesInteractor
import io.github.yutakax17.advancedhelloworld.core.Clock
import io.github.yutakax17.advancedhelloworld.core.SyncContributor
import io.github.yutakax17.advancedhelloworld.core.UuidGenerator
import io.github.yutakax17.advancedhelloworld.core.network.BackendConfiguration
import io.github.yutakax17.advancedhelloworld.core.network.createBackendHttpClient
import io.github.yutakax17.advancedhelloworld.messages.KtorMessageRemoteDataSource
import io.github.yutakax17.advancedhelloworld.messages.MessageSynchronizationEngine
import io.github.yutakax17.advancedhelloworld.messages.SqlDelightMessageRepository
import io.github.yutakax17.advancedhelloworld.messages.database.MessagesDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.Closeable
import java.util.UUID

public class AdvancedHelloWorldApplication : Application() {
    private val messagesStore: AndroidMessagesStore by lazy {
        AndroidMessagesStore(applicationContext)
    }

    internal val messagesInteractor: MessagesInteractor
        get() = messagesStore.interactor

    internal val messagesSyncContributor: SyncContributor
        get() = messagesStore.syncContributor

    override fun onCreate() {
        super.onCreate()
        MessageSyncScheduler.enqueue(this)
    }
}

/** Owns the process-scoped SQLDelight driver and durable messages composition root. */
internal class AndroidMessagesStore(
    context: android.content.Context,
    databaseName: String = DATABASE_NAME,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
    clock: Clock = Clock(System::currentTimeMillis),
    uuidGenerator: UuidGenerator = UuidGenerator { UUID.randomUUID().toString() },
    httpClient: HttpClient =
        createBackendHttpClient(
            engine = OkHttp.create(),
            configuration = BackendConfiguration(BuildConfig.API_BASE_URL),
        ),
) : Closeable {
    private val httpClient = httpClient
    private val remoteDataSource =
        KtorMessageRemoteDataSource(
            client = httpClient,
            baseUrl = BuildConfig.API_BASE_URL,
        )
    private val driver = AndroidSqliteDriver(MessagesDatabase.Schema, context, databaseName)
    internal val database = MessagesDatabase(driver)
    internal val syncContributor: SyncContributor =
        MessageSynchronizationEngine(
            database = database,
            remote = remoteDataSource,
            clock = clock,
            uuidGenerator = uuidGenerator,
        )
    internal val repository =
        SqlDelightMessageRepository(
            database = database,
            clock = clock,
            uuidGenerator = uuidGenerator,
            queryDispatcher = queryDispatcher,
            syncContributor = syncContributor,
        )
    internal val interactor: MessagesInteractor = RepositoryMessagesInteractor(repository)

    override fun close() {
        httpClient.close()
        driver.close()
    }

    private companion object {
        const val DATABASE_NAME: String = "advanced-hello-world.db"
    }
}
