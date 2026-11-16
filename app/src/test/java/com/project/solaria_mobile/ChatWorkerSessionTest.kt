package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.database.AppDatabase
import com.project.solaria_mobile.core.database.ChatDatabaseRouter
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.di.DatabaseModule
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import com.project.solaria_mobile.feature.communication.data.MessageStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatWorkerSessionTest {
    private var token: String? = null
    private val enabled = MutableStateFlow(false)
    private val mode = mockk<MockModeStore> {
        every { isEnabled() } answers { enabled.value }
        every { enabled } returns this@ChatWorkerSessionTest.enabled
    }
    private val pending = InMemoryPendingMessageDao()
    private val conversations = InMemoryConversationDao()
    private val messages = InMemoryMessageDao(conversations)
    private val real = mockk<AppDatabase> {
        every { pendingMessages() } returns pending
        every { conversations() } returns this@ChatWorkerSessionTest.conversations
        every { messages() } returns this@ChatWorkerSessionTest.messages
    }
    private val credentials = object : CredentialProvider { override fun accessToken() = token }
    private val router = ChatDatabaseRouter(real, mockk(), mode, credentials)

    @Test fun workerStartedWithoutSessionCanSendAfterLoginAndAfterAccountSwitch() = runTest {
        val api = ScriptedMessengerApi()
        val outbox = MessageOutbox(
            DatabaseModule.pending(router), api, ApiCallExecutor(ApiErrorParser(ApiJson.instance)),
            object : NetworkMonitor { override val isOnline = MutableStateFlow(true) }, credentials,
            MessageStore(DatabaseModule.messages(router), DatabaseModule.conversations(router)), backgroundScope, mode, router,
        )
        outbox.start()
        runCurrent()
        token = tokenFor("user-1")
        assertEquals(ApiResult.Success(Unit), outbox.enqueue("c1", "USER_TO_USER", "primeira"))
        runCurrent()
        token = tokenFor("user-2")
        assertEquals(ApiResult.Success(Unit), outbox.enqueue("c2", "USER_TO_USER", "segunda"))
        runCurrent()
        assertEquals(listOf("primeira", "segunda"), api.sendRequests.map { it.content })
        assertTrue(pending.rows.isEmpty())
    }

    @Test fun modeSwitchDuringReplyDoesNotDeleteTheOriginalPendingMessageOrWriteIntoMock() = runTest {
        token = tokenFor("user-1")
        val api = ScriptedMessengerApi().apply { beforeReply = { enabled.value = true } }
        val outbox = MessageOutbox(
            DatabaseModule.pending(router), api, ApiCallExecutor(ApiErrorParser(ApiJson.instance)),
            object : NetworkMonitor { override val isOnline = MutableStateFlow(true) }, credentials,
            MessageStore(DatabaseModule.messages(router), DatabaseModule.conversations(router)), backgroundScope, mode, router,
        )
        outbox.start()
        runCurrent()
        outbox.enqueue("c1", "USER_TO_USER", "preservar")
        runCurrent()
        assertEquals(listOf("preservar"), pending.rows.map { it.content })
        assertTrue(messages.rows.isEmpty())
    }
}
