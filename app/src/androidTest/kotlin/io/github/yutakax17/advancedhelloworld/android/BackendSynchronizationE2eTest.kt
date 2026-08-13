package io.github.yutakax17.advancedhelloworld.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import io.github.yutakax17.advancedhelloworld.core.SyncResult
import io.github.yutakax17.advancedhelloworld.messages.CreateMessageResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
class BackendSynchronizationE2eTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "messages-e2e.db"
    private val journeyId = System.currentTimeMillis().toString()

    @Before
    fun clearDatabase() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun offlineMessageSurvivesRestartUploadsAndRetrievesBackendMessage() = runBlocking {
        val offlineText = "android-offline-$journeyId"
        val backendText = "backend-originated-$journeyId"

        AndroidMessagesStore(context, databaseName).use { store ->
            assertTrue(store.repository.createOffline(offlineText) is CreateMessageResult.Created)
        }

        AndroidMessagesStore(context, databaseName).use { restartedStore ->
            assertTrue(restartedStore.repository.listLocal().any { it.text == offlineText })
            assertEquals(SyncResult.Success, restartedStore.syncContributor.synchronize())
        }

        postBackendMessage(backendText)

        AndroidMessagesStore(context, databaseName).use { refreshedStore ->
            assertEquals(SyncResult.Success, refreshedStore.syncContributor.synchronize())
            val messages = refreshedStore.repository.listLocal()
            assertTrue(messages.any { it.text == offlineText && it.remoteId != null })
            assertTrue(messages.any { it.text == backendText && it.remoteId != null })
        }
    }

    private fun postBackendMessage(text: String) {
        val connection = URL("${BuildConfig.API_BASE_URL}/api/v1/messages").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.bufferedWriter().use { writer -> writer.write("{\"text\":\"$text\"}") }
            assertEquals(201, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }
}
