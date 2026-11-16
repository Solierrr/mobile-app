package com.project.solaria_mobile

import com.project.solaria_mobile.core.database.AppDatabase
import com.project.solaria_mobile.core.database.ChatContextChanged
import com.project.solaria_mobile.core.database.ChatDatabaseRouter
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.CredentialProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDatabaseRoutingTest {
    private val enabled = MutableStateFlow(false)
    private var actor = "actor-one"
    private val real = mockk<AppDatabase>()
    private val mock = mockk<AppDatabase>()
    private val mode = mockk<MockModeStore> {
        every { isEnabled() } answers { enabled.value }
        every { enabled } returns this@ChatDatabaseRoutingTest.enabled
    }
    private val credentials = object : CredentialProvider {
        override fun accessToken() = "h." + java.util.Base64.getUrlEncoder().encodeToString("""{"sub":"$actor"}""".toByteArray()) + ".s"
    }
    private val router = ChatDatabaseRouter(real, mock, mode, credentials)

    @Test fun switchingModeRejectsLateWriteWithoutRedirectingItToOtherDatabase() = runTest {
        val gate = CompletableDeferred<Unit>()
        val write = async {
            router.session {
                assertSame(real, router.forOperation())
                gate.await()
                try { router.forOperation(); false } catch (_: ChatContextChanged) { true }
            }
        }
        advanceUntilIdle()
        enabled.value = true
        gate.complete(Unit)
        assertTrue(write.await())
        assertSame(mock, router.current())
        enabled.value = false
        assertSame(real, router.current())
    }

    @Test fun accountSwitchRejectsLateWriteEvenWhenModeIsUnchanged() = runTest {
        router.session {
            actor = "actor-two"
            val rejected = try { router.forOperation(); false } catch (_: ChatContextChanged) { true }
            assertTrue(rejected)
        }
    }

    @Test fun observersMoveToTheSelectedDatabaseWithoutReusingOtherModeHistory() = runTest {
        val observed = mutableListOf<String>()
        backgroundScope.launch { router.observe { flowOf(if (it === real) "real" else "mock") }.collect { observed += it } }
        runCurrent()
        enabled.value = true
        runCurrent()
        enabled.value = false
        runCurrent()
        assertEquals(listOf("real", "mock", "real"), observed)
    }
}
