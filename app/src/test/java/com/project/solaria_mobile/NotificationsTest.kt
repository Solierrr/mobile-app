package com.project.solaria_mobile

import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.communication.NotificationsViewModel
import com.project.solaria_mobile.feature.communication.data.NotificationUpdates
import com.project.solaria_mobile.feature.communication.domain.AccountNotification
import com.project.solaria_mobile.feature.communication.domain.NotificationPage
import com.project.solaria_mobile.feature.communication.domain.NotificationRepository
import com.project.solaria_mobile.feature.communication.domain.NotificationTarget
import com.project.solaria_mobile.feature.communication.domain.target
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun emptyInboxStillHasContentAndFilterState() = runTest {
        val viewModel = NotificationsViewModel(FakeNotifications(mutableListOf()), NotificationUpdates())
        viewModel.uiState.test {
            advanceUntilIdle()
            val content = (expectMostRecentItem() as UiState.Content).data
            assertTrue(content.items.isEmpty())
            assertEquals(0L, content.unreadCount)
            viewModel.filter(true)
            advanceUntilIdle()
            assertTrue((expectMostRecentItem() as UiState.Content).data.unreadOnly)
        }
    }

    @Test fun failedReadKeepsUnreadItemAndDoesNotOpenResource() = runTest {
        val repository = FakeNotifications().apply { readFailure = true }
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.openings.test {
            viewModel.uiState.test {
                advanceUntilIdle()
                val item = (expectMostRecentItem() as UiState.Content).data.items.first()
                viewModel.open(item)
                advanceUntilIdle()
                val content = (expectMostRecentItem() as UiState.Content).data
                assertNull(content.items.first().readAt)
                assertNull(content.pendingId)
                assertEquals(ErrorKind.SERVER, content.actionError)
            }
            expectNoEvents()
        }
    }

    @Test fun refreshDuringReadDoesNotLeaveTheItemPermanentlyBusy() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = FakeNotifications().apply { readGate = release }
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.uiState.test {
            advanceUntilIdle()
            val item = (expectMostRecentItem() as UiState.Content).data.items.first()
            viewModel.open(item)
            runCurrent()
            viewModel.load()
            viewModel.filter(true)
            release.complete(Unit)
            advanceUntilIdle()
            val content = (expectMostRecentItem() as UiState.Content).data
            assertNull(content.pendingId)
            assertNotNull(content.items.first().readAt)
            assertEquals(1, repository.readCalls)
        }
    }

    @Test fun failedPaginationPreservesItemsAndRetryDoesNotDuplicateThem() = runTest {
        val repository = FakeNotifications(MutableList(5) { notification("n-$it") }).apply { pageFailure = true }
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(2, (expectMostRecentItem() as UiState.Content).data.items.size)
            viewModel.loadMore()
            advanceUntilIdle()
            val failed = (expectMostRecentItem() as UiState.Content).data
            assertEquals(2, failed.items.size)
            assertEquals(0, failed.page)
            assertFalse(failed.loadingMore)
            repository.pageFailure = false
            viewModel.loadMore()
            advanceUntilIdle()
            val recovered = (expectMostRecentItem() as UiState.Content).data
            assertEquals(4, recovered.items.size)
            assertEquals(1, recovered.page)
            assertEquals(recovered.items.size, recovered.items.distinctBy { it.id }.size)
        }
    }

    @Test fun refreshInProgressDoesNotStartReadMutationsOrLoseUnreadItems() = runTest {
        val repository = FakeNotifications()
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.openings.test {
            viewModel.uiState.test {
                advanceUntilIdle()
                val original = (expectMostRecentItem() as UiState.Content).data.items.first()
                val release = CompletableDeferred<Unit>()
                repository.pageGate = release
                viewModel.load()
                runCurrent()
                assertTrue((expectMostRecentItem() as UiState.Content).refreshing)
                viewModel.open(original)
                viewModel.markAllRead()
                runCurrent()
                assertEquals(0, repository.readCalls)
                assertEquals(0, repository.readAllCalls)
                release.complete(Unit)
                advanceUntilIdle()
                val refreshed = expectMostRecentItem() as UiState.Content
                assertFalse(refreshed.refreshing)
                assertNull(refreshed.data.items.first().readAt)
                assertNull(refreshed.data.pendingId)
                assertFalse(refreshed.data.markingAll)
            }
            expectNoEvents()
        }
    }

    @Test fun readAllRequeriesCounterAndKeepsExistingReadTimestamp() = runTest {
        val previous = "2026-10-27T10:00:00Z"
        val repository = FakeNotifications(mutableListOf(notification("one"), notification("two").copy(readAt = previous)))
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            viewModel.markAllRead()
            advanceUntilIdle()
            val content = (expectMostRecentItem() as UiState.Content).data
            assertEquals(0L, content.unreadCount)
            assertEquals(previous, content.items.first { it.id == "two" }.readAt)
            assertFalse(content.markingAll)
        }
    }

    @Test fun pushOpensAuthorizedNotificationOutsideTheLoadedPage() = runTest {
        val repository = FakeNotifications(MutableList(4) { notification("n-$it") })
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.openings.test {
            viewModel.uiState.test {
                advanceUntilIdle()
                val initial = (expectMostRecentItem() as UiState.Content).data
                assertFalse(initial.items.any { it.id == "n-3" })
                viewModel.openById("n-3")
                advanceUntilIdle()
                val refreshed = (expectMostRecentItem() as UiState.Content).data
                assertEquals(1, repository.readCalls)
                assertNull(refreshed.pendingId)
                assertNotNull(repository.items.first { it.id == "n-3" }.readAt)
            }
            val opened = awaitItem()
            assertEquals("n-3", opened.id)
            assertNotNull(opened.readAt)
            expectNoEvents()
        }
    }

    @Test fun forbiddenPushDestinationNeverEmitsAnOpening() = runTest {
        val repository = FakeNotifications().apply { readError = ErrorKind.FORBIDDEN }
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.openings.test {
            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()
                viewModel.openById("another-account-notification")
                advanceUntilIdle()
                val content = (expectMostRecentItem() as UiState.Content).data
                assertEquals(1, repository.readCalls)
                assertEquals(ErrorKind.FORBIDDEN, content.actionError)
                assertNull(content.pendingId)
                assertNull(repository.items.first().readAt)
            }
            expectNoEvents()
        }
    }

    @Test fun failedPushOpeningCanRetryAndDeliversTheAuthorizedItemOnce() = runTest {
        val firstRequest = CompletableDeferred<Unit>()
        val repository = FakeNotifications(MutableList(4) { notification("n-$it") }).apply {
            readError = ErrorKind.CONNECTIVITY
            readGate = firstRequest
        }
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        viewModel.openings.test {
            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()
                viewModel.openById("n-3")
                runCurrent()
                assertEquals("n-3", (expectMostRecentItem() as UiState.Content).data.pendingId)
                firstRequest.complete(Unit)
                advanceUntilIdle()
                val failed = (expectMostRecentItem() as UiState.Content).data
                assertEquals(ErrorKind.CONNECTIVITY, failed.actionError)
                assertEquals("n-3", failed.retryOpenId)
                assertNull(failed.pendingId)
                val retryRequest = CompletableDeferred<Unit>()
                repository.readError = null
                repository.readGate = retryRequest
                viewModel.openById(requireNotNull(failed.retryOpenId))
                runCurrent()
                val retrying = (expectMostRecentItem() as UiState.Content).data
                assertEquals("n-3", retrying.pendingId)
                assertNull(retrying.actionError)
                assertNull(retrying.retryOpenId)
                retryRequest.complete(Unit)
                advanceUntilIdle()
                val recovered = (expectMostRecentItem() as UiState.Content).data
                assertNull(recovered.pendingId)
                assertNull(recovered.actionError)
                assertNull(recovered.retryOpenId)
                assertEquals(2, repository.readCalls)
            }
            assertEquals("n-3", awaitItem().id)
            expectNoEvents()
        }
    }

    @Test fun openingReadItemBeforeCollectorStartsIsDeliveredExactlyOnce() = runTest {
        val item = notification("already-read").copy(readAt = "2026-10-28T09:30:00Z")
        val repository = FakeNotifications(mutableListOf(item))
        val viewModel = NotificationsViewModel(repository, NotificationUpdates())
        advanceUntilIdle()
        viewModel.open(item)
        advanceUntilIdle()
        assertEquals(0, repository.readCalls)
        viewModel.openings.test {
            assertEquals(item, awaitItem())
            expectNoEvents()
        }
        viewModel.openings.test { expectNoEvents() }
    }

    @Test fun targetsUseKnownIdsAndNeverArbitraryUrls() {
        assertEquals(NotificationTarget.Conversation("chat-1"), notification("one").copy(data = mapOf("chatId" to "chat-1")).target())
        assertNull(notification("two").copy(data = mapOf("chatId" to "https://outside.example/secret")).target())
        assertNull(notification("three").copy(data = mapOf("chatId" to "../../other")).target())
        assertEquals(NotificationTarget.Service("service-1"), notification("four").copy(type = "SERVICE_SCHEDULED", data = mapOf("technicalServiceId" to "service-1")).target())
    }

    private class FakeNotifications(val items: MutableList<AccountNotification> = mutableListOf(notification("one"))) : NotificationRepository {
        var readFailure = false
        var readError: ErrorKind? = null
        var pageFailure = false
        var readGate: CompletableDeferred<Unit>? = null
        var pageGate: CompletableDeferred<Unit>? = null
        var readCalls = 0
        var readAllCalls = 0
        override suspend fun page(page: Int, unreadOnly: Boolean): ReadResult<NotificationPage> {
            pageGate?.await()
            if (page > 0 && pageFailure) return ReadResult(ApiResult.Failure(ErrorKind.CONNECTIVITY))
            val filtered = items.filter { !unreadOnly || it.readAt == null }
            return ReadResult(ApiResult.Success(NotificationPage(filtered.drop(page * 2).take(2), page, (filtered.size + 1) / 2)))
        }
        override suspend fun unreadCount(): ApiResult<Long> = ApiResult.Success(items.count { it.readAt == null }.toLong())
        override suspend fun markRead(id: String): ApiResult<AccountNotification> {
            readCalls++
            readGate?.await()
            readError?.let { return ApiResult.Failure(it) }
            if (readFailure) return ApiResult.Failure(ErrorKind.SERVER)
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) return ApiResult.Failure(ErrorKind.NOT_FOUND)
            items[index] = items[index].copy(readAt = items[index].readAt ?: "2026-10-28T10:00:00Z")
            return ApiResult.Success(items[index])
        }
        override suspend fun markAllRead(): ApiResult<Unit> {
            readAllCalls++
            items.indices.forEach { items[it] = items[it].copy(readAt = items[it].readAt ?: "2026-10-28T10:00:00Z") }
            return ApiResult.Success(Unit)
        }
    }

    companion object {
        private fun notification(id: String) = AccountNotification(id, "NEW_MESSAGE", "Nova mensagem", "Confira a conversa", emptyMap(), "2026-10-28T09:00:00Z")
    }
}
