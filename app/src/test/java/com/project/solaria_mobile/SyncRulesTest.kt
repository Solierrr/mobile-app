package com.project.solaria_mobile

import com.project.solaria_mobile.core.database.ConversationEntity
import com.project.solaria_mobile.core.database.MessageEntity
import com.project.solaria_mobile.core.database.MessageStats
import com.project.solaria_mobile.core.database.hasHole
import com.project.solaria_mobile.core.database.mergeConversation
import com.project.solaria_mobile.core.database.mergeMessage
import com.project.solaria_mobile.feature.communication.data.HistoryStep
import com.project.solaria_mobile.feature.communication.data.nextHistoryStep
import com.project.solaria_mobile.feature.communication.data.parseMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes das regras puras da sincronização: versão maior vence, exclusão apaga o texto, troca de época, buraco de
 * `sequence` e o próximo pedido de histórico. Também passa as regras pelos DAOs em memória, que usam o mesmo código
 * de produção dentro de `upsert`, `applyList` e `remove`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRulesTest {
    private fun message(version: Long, content: String? = "texto", deleted: Boolean = false, id: String = "m1", sequence: Int = 1) =
        MessageEntity(id, "c1", sequence, "u2", null, content, false, deleted, 0, version)

    private fun conversation(
        id: String = "c1",
        epoch: String = "e1",
        version: Long = 5,
        lost: Boolean = false,
    ) = ConversationEntity(id, epoch, version, "GROUP", "u1,u2", "Grupo", false, 1, null, 0, null, false, lost)

    private fun stats(count: Int, min: Int?, max: Int?) = MessageStats(count, min, max)

    @Test fun newMessageIsInserted() {
        val incoming = message(version = 3)

        assertEquals(incoming, mergeMessage(null, incoming))
    }

    @Test fun higherVersionReplacesTheStoredMessage() {
        val result = mergeMessage(message(version = 3, content = "antes"), message(version = 4, content = "depois"))

        assertEquals("depois", result?.content)
    }

    @Test fun equalOrLowerVersionIsIgnored() {
        val stored = message(version = 4, content = "atual")

        assertNull(mergeMessage(stored, message(version = 4, content = "mesma versão")))
        assertNull(mergeMessage(stored, message(version = 3, content = "página velha")))
    }

    @Test fun legacyMessageWithVersionZeroIsStoredOnceAndNeverReplacedByItself() {
        val legacy = message(version = 0)

        assertEquals(legacy, mergeMessage(null, legacy))
        assertNull(mergeMessage(legacy, legacy))
    }

    @Test fun tombstoneErasesTheTextEvenIfItArrivesWithContent() {
        val result = mergeMessage(message(version = 3, content = "segredo"), message(version = 4, content = "ainda veio texto", deleted = true))

        assertTrue(result!!.deleted)
        assertNull(result.content)
    }

    @Test fun staleCopyCannotBringBackTheTextOfADeletedMessage() {
        val tombstone = message(version = 4, content = null, deleted = true)

        assertNull(mergeMessage(tombstone, message(version = 3, content = "segredo")))
    }

    @Test fun newConversationIsInserted() {
        assertEquals(conversation(), mergeConversation(null, conversation()))
    }

    @Test fun conversationWithHigherVersionInTheSameEpochReplacesTheStoredOne() {
        assertEquals(6L, mergeConversation(conversation(version = 5), conversation(version = 6))?.syncVersion)
    }

    @Test fun conversationWithEqualOrLowerVersionIsIgnored() {
        assertNull(mergeConversation(conversation(version = 5), conversation(version = 5)))
        assertNull(mergeConversation(conversation(version = 5), conversation(version = 4)))
    }

    @Test fun newEpochWinsEvenWithALowerVersion() {
        val result = mergeConversation(conversation(epoch = "e1", version = 900), conversation(epoch = "e2", version = 1))

        assertEquals("e2", result?.syncEpoch)
    }

    @Test fun updateThatWinsClearsTheLostAccessMark() {
        val result = mergeConversation(conversation(version = 5, lost = true), conversation(version = 6, lost = false))

        assertFalse(result!!.accessLost)
    }

    @Test fun updateThatLosesKeepsTheLostAccessMark() {
        assertNull(mergeConversation(conversation(version = 5, lost = true), conversation(version = 4, lost = false)))
    }

    @Test fun holeIsDetectedFromTheCountAndTheSequenceRange() {
        assertFalse(stats(5, 1, 5).hasHole)
        assertTrue(stats(4, 1, 5).hasHole)
        assertFalse(stats(0, null, null).hasHole)
        assertFalse(stats(3, 98, 100).hasHole)
    }

    @Test fun emptyConversationWithMessagesOnTheServerAsksForTheLatest() {
        assertEquals(HistoryStep.Latest, nextHistoryStep(stats(0, null, null), holeAfter = null, headSequence = 40))
    }

    @Test fun emptyConversationWithoutMessagesIsDone() {
        assertEquals(HistoryStep.Done, nextHistoryStep(stats(0, null, null), holeAfter = null, headSequence = 0))
    }

    @Test fun holeIsFilledBeforeAnythingElse() {
        assertEquals(HistoryStep.Since(3), nextHistoryStep(stats(4, 1, 6), holeAfter = 3, headSequence = 9))
    }

    @Test fun newerMessagesOnTheServerAreFetchedBeforeTheOldHistory() {
        assertEquals(HistoryStep.Since(50), nextHistoryStep(stats(10, 41, 50), holeAfter = null, headSequence = 55))
    }

    @Test fun olderHistoryIsDownloadedBackwardsUntilSequenceOne() {
        assertEquals(HistoryStep.Before(41), nextHistoryStep(stats(10, 41, 50), holeAfter = null, headSequence = 50))
        assertEquals(HistoryStep.Done, nextHistoryStep(stats(50, 1, 50), holeAfter = null, headSequence = 50))
    }

    @Test fun messageLiveAheadOfTheListPreviewDoesNotAskForMore() {
        assertEquals(HistoryStep.Done, nextHistoryStep(stats(52, 1, 52), holeAfter = null, headSequence = 50))
    }

    @Test fun serverInstantsBecomeMillisAndBadValuesBecomeNull() {
        assertEquals(1_793_181_600_123L, parseMillis("2026-10-28T10:00:00.123Z"))
        assertNull(parseMillis(null))
        assertNull(parseMillis("ontem"))
    }

    @Test fun messageDaoUpsertAppliesTheVersionRuleAndReportsWhatChanged() = runTest {
        val dao = InMemoryMessageDao(InMemoryConversationDao())
        dao.upsert(listOf(message(version = 2, content = "v2")))

        val applied = dao.upsert(listOf(message(version = 1, content = "velha"), message(version = 3, content = "v3"), message(version = 1, id = "m2", sequence = 2)))

        assertEquals(listOf("v3", "texto"), applied.map { it.content })
        assertEquals(listOf("v3", "texto"), dao.rows.sortedBy { it.sequence }.map { it.content })
    }

    @Test fun conversationListMarksWhatIsMissingAsLostButSkipsConversationsNewerThanTheList() = runTest {
        val dao = InMemoryConversationDao()
        dao.upsert(listOf(conversation("antiga", version = 3), conversation("nova-por-evento", version = 50), conversation("volta", version = 3, lost = true)))

        dao.applyList(listOf(conversation("volta", version = 10), conversation("fica", version = 10)))

        val byId = dao.rows.associateBy { it.id }
        assertTrue(byId.getValue("antiga").accessLost)
        assertFalse("criada depois da lista: não pode ser tomada por perdida", byId.getValue("nova-por-evento").accessLost)
        assertFalse(byId.getValue("volta").accessLost)
        assertFalse(byId.getValue("fica").accessLost)
    }

    @Test fun emptyConversationListMarksEverythingAsLost() = runTest {
        val dao = InMemoryConversationDao()
        dao.upsert(listOf(conversation("a"), conversation("b")))

        dao.applyList(emptyList())

        assertTrue(dao.rows.all { it.accessLost })
    }

    @Test fun removalOnlyCountsIfItIsNewerAndIgnoresUnknownConversations() = runTest {
        val dao = InMemoryConversationDao()
        dao.upsert(listOf(conversation("c1", version = 5)))

        dao.remove("c1", "e1", 4)
        assertFalse(dao.rows.single().accessLost)
        dao.remove("desconhecida", "e1", 9)
        assertEquals(1, dao.rows.size)
        dao.remove("c1", "e1", 6)
        assertTrue(dao.rows.single().accessLost)
        assertEquals(6L, dao.rows.single().syncVersion)
    }
}
