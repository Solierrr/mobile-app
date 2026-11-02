package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.hasSequenceGap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun message(id: String, version: Long) = MessageDto(id, "c1", "ana", "texto $id", changeVersion = version, sequence = version.toInt())

private fun page(epoch: String, cursor: Long, hasMore: Boolean = false, vararg messages: MessageDto) =
    ApiResult.Success(MessageUpdatesDto(epoch, cursor, hasMore, messages.toList()))

/** Recuperação por versão: o ponto de partida, a paginação, a falha que não perde o cursor e a época que muda. */
class UpdatesMessageRecoveryTest {
    private val asked = mutableListOf<Long?>()
    private val answers = ArrayDeque<ApiResult<MessageUpdatesDto>>()
    private val recovery = UpdatesMessageRecovery { since -> asked += since; answers.removeFirst() }
    private val delivered = mutableListOf<ChatEvent>()
    private suspend fun run() = recovery.recover { delivered += it }

    @Test fun theFirstCallOnlyMarksTheStartingPoint() = runTest {
        answers += page("e1", 100)
        run()
        assertEquals(listOf<Long?>(null), asked)
        assertTrue(delivered.isEmpty())
    }

    @Test fun nextCallsAskSinceTheCursorAndDeliverInOrder() = runTest {
        answers += page("e1", 100)
        answers += page("e1", 130, false, message("m1", 110), message("m2", 130))
        answers += page("e1", 130)
        run()
        run()
        run()
        assertEquals(listOf<Long?>(null, 100, 130), asked)
        assertEquals(listOf("m1", "m2"), delivered.map { (it as ChatEvent.MessageRecovered).message.id })
    }

    @Test fun pagesAreFollowedWhileThereIsMore() = runTest {
        answers += page("e1", 100)
        answers += page("e1", 120, true, message("m1", 120))
        answers += page("e1", 150, false, message("m2", 150))
        run()
        run()
        assertEquals(listOf<Long?>(null, 100, 120), asked)
        assertEquals(listOf("m1", "m2"), delivered.map { (it as ChatEvent.MessageRecovered).message.id })
    }

    @Test fun aFailureKeepsTheCursorSoTheNextConnectionTriesAgain() = runTest {
        answers += page("e1", 100)
        answers += ApiResult.Failure(ErrorKind.CONNECTIVITY)
        answers += page("e1", 140, false, message("m1", 140))
        run()
        run()
        assertTrue(delivered.isEmpty())
        run()
        assertEquals(listOf<Long?>(null, 100, 100), asked)
        assertEquals(1, delivered.size)
    }

    @Test fun aFailedStartingPointIsRequestedAgain() = runTest {
        answers += ApiResult.Failure(ErrorKind.SERVER)
        answers += page("e1", 100)
        run()
        run()
        assertEquals(listOf<Long?>(null, null), asked)
    }

    @Test fun aNewEpochAsksForResyncAndTakesANewStartingPoint() = runTest {
        answers += page("e1", 100)
        answers += page("e2", 7, false, message("old", 3))
        answers += page("e2", 9)
        answers += page("e2", 12, false, message("m1", 12))
        run()
        run()
        assertEquals(listOf<ChatEvent>(ChatEvent.Resynced), delivered)
        run()
        assertEquals(listOf<Long?>(null, 100, null, 9), asked)
        assertEquals(ChatEvent.Resynced, delivered.first())
        assertEquals("m1", (delivered.last() as ChatEvent.MessageRecovered).message.id)
    }
}

class SequenceGapTest {
    @Test fun theNextSequenceIsNotAGap() = assertFalse(hasSequenceGap(lastKnown = 5, incoming = 6))
    @Test fun aSkippedSequenceIsAGap() = assertTrue(hasSequenceGap(lastKnown = 5, incoming = 7))
    @Test fun anOldOrRepeatedSequenceIsNotAGap() {
        assertFalse(hasSequenceGap(5, 5))
        assertFalse(hasSequenceGap(5, 3))
    }
    @Test fun withoutAKnownMessageThereIsNothingToCompare() {
        assertFalse(hasSequenceGap(0, 9))
        assertFalse(hasSequenceGap(5, 0))
    }
}
