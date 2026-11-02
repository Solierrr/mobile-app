package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.feature.communication.ConversationsViewModel
import com.project.solaria_mobile.feature.communication.GroupDetailsViewModel
import com.project.solaria_mobile.feature.communication.NewGroup
import com.project.solaria_mobile.feature.communication.NewGroupViewModel
import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.GroupConversationRequest
import com.project.solaria_mobile.feature.communication.data.GroupCreationDto
import com.project.solaria_mobile.feature.communication.data.MediaGateway
import com.project.solaria_mobile.feature.communication.data.MediaUploadTarget
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.feature.communication.data.ParticipantNames
import com.project.solaria_mobile.feature.communication.data.ParticipantsRequest
import com.project.solaria_mobile.feature.communication.data.PresenceState
import com.project.solaria_mobile.feature.communication.data.RejectedGroupParticipantDto
import com.project.solaria_mobile.feature.communication.data.RemoteCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.TransferManagerRequest
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.GroupCreated
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val ME = "a1111111-1111-1111-1111-111111111111"
private const val ANA = "22222222-2222-2222-2222-222222222222"
private const val BIA = "33333333-3333-3333-3333-333333333333"

private val token = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$ME"}""".toByteArray()) + ".s"
private val credentials = object : CredentialProvider { override fun accessToken() = token }


/** Grupo avulso, arquivadas e o contrato de erros do messenger, vistos pelo repositório. */
class MessengerGroupsRepositoryTest {
    private fun repository(api: MessengerApi, names: ParticipantNames = ParticipantNames { null }) =
        RemoteCommunicationRepository(api, ApiCallExecutor(ApiErrorParser(ApiJson.instance)), credentials, names, com.project.solaria_mobile.feature.communication.data.NoMediaGateway)

    @Test fun errorCodeComesFromTheProblemDetail() {
        val parser = ApiErrorParser(ApiJson.instance)
        assertEquals("SUCCESSOR_REQUIRED", parser.parse(409, """{"errorCode":"SUCCESSOR_REQUIRED","detail":"x"}""").errorCode)
        assertEquals("MEDIA_EXPIRED", parser.parse(410, """{"errorCode":"MEDIA_EXPIRED"}""").errorCode)
        assertNull(parser.parse(409, "não é json").errorCode)
        assertNull(parser.parse(500, null).errorCode)
        assertEquals(ErrorKind.CONFLICT, parser.parse(409, """{"errorCode":"SUCCESSOR_INELIGIBLE"}""").kind)
    }

    @Test fun createGroupCountsTheRejectedGuests() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.createGroup(GroupConversationRequest("Equipe", setOf(ANA, BIA))) } returns GroupCreationDto(
            ConversationDto("g1", "GROUP", title = "Equipe"), rejectedParticipants = listOf(RejectedGroupParticipantDto(BIA, "USER_INACTIVE")),
        )
        assertEquals(ApiResult.Success(GroupCreated("g1", 1)), repository(api).createGroup("  Equipe ", setOf(ANA, BIA)))
    }

    @Test fun addingParticipantsReturnsHowManyWereRefused() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.addParticipants("g1", ParticipantsRequest(setOf(ANA))) } returns
            ConversationDto("g1", "GROUP", rejectedParticipants = listOf(RejectedGroupParticipantDto(ANA, "USER_NOT_FOUND")))
        assertEquals(ApiResult.Success(1), repository(api).addParticipants("g1", setOf(ANA)))
    }

    @Test fun leavingAsManagerSendsTheSuccessorAndManagerTransferHasItsBody() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.removeParticipant("g1", ME, ANA) } returns Unit
        coEvery { api.transferManager("g1", TransferManagerRequest(ANA)) } returns kotlinx.serialization.json.JsonObject(emptyMap())
        val repository = repository(api)
        assertEquals(ApiResult.Success(Unit), repository.removeParticipant("g1", ME, ANA))
        assertEquals(ApiResult.Success(Unit), repository.transferManager("g1", ANA))
        coVerify(exactly = 1) { api.removeParticipant("g1", ME, ANA) }
    }

    @Test fun archivedListAsksTheServerForTheArchivedOnesAndNamesTheDirects() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversations(true) } returns listOf(ConversationDto("d1", "DIRECT", participantIds = listOf(ME, ANA), unreadCount = 0))
        val list = (repository(api, ParticipantNames { "Ana" }).archivedConversations() as ApiResult.Success).data
        assertEquals(listOf("Ana"), list.map { it.name })
        coVerify(exactly = 0) { api.conversations(false) }
    }

    @Test fun managerIdComesFromTheServerInLowercase() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversation("g1") } returns ConversationDto("g1", "GROUP", title = "Equipe", participantIds = listOf(ME, ANA), managerUserId = ME.uppercase())
        val conversation = (repository(api).conversation("g1") as ApiResult.Success).data
        assertEquals(ME, conversation.managerId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class Groups(var manager: String? = ME) : CommunicationRepository by FakeCommunicationRepository() {
        val removed = mutableListOf<Triple<String, String, String?>>()
        var members = listOf(ME, ANA, BIA)
        var failure: ApiResult.Failure? = null

        override fun currentUserId() = ME
        override suspend fun conversation(id: String): ApiResult<Conversation> = ApiResult.Success(
            Conversation(id, "EQ", "Equipe", "", "", AvatarTone.BLUE, group = true, managerId = manager, participants = members.toSet(), participantNames = mapOf(ANA to "Ana", BIA to "Bia")),
        )
        override suspend fun removeParticipant(id: String, userId: String, successorId: String?): ApiResult<Unit> {
            removed += Triple(id, userId, successorId)
            failure?.let { return it }
            members = members - userId
            return ApiResult.Success(Unit)
        }
        override suspend fun addParticipants(id: String, userIds: Set<String>): ApiResult<Int> { members = members + userIds; return ApiResult.Success(1) }
        override suspend fun transferManager(id: String, successorId: String): ApiResult<Unit> { manager = successorId; return ApiResult.Success(Unit) }
    }

    private fun viewModel(repository: CommunicationRepository) = GroupDetailsViewModel(SavedStateHandle(mapOf("id" to "g1")), repository)

    @Test fun listsTheManagerFirstThenYouAndMarksWhoCanManage() = runTest {
        val viewModel = viewModel(Groups())
        viewModel.uiState.test {
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertEquals(listOf(ME, ANA, BIA), ui.members.map { it.authId })
            assertEquals(listOf(null, "Ana", "Bia"), ui.members.map { it.name })
            assertTrue(ui.canManage)
            assertTrue(ui.members.first().manager && ui.members.first().self)
            assertEquals(listOf(ANA, BIA), ui.successors.map { it.authId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun aParticipantWhoIsNotTheManagerCannotManage() = runTest {
        val viewModel = viewModel(Groups(manager = ANA))
        viewModel.uiState.test {
            advanceUntilIdle()
            assertFalse((expectMostRecentItem() as UiState.Content).data.canManage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun removingSomeoneReloadsTheGroupWithoutThem() = runTest {
        val repository = Groups()
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            viewModel.remove(BIA)
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertEquals(listOf(ME, ANA), ui.members.map { it.authId })
            assertFalse(ui.left)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(Triple("g1", BIA, null)), repository.removed)
    }

    @Test fun leavingSendsTheOwnIdWithTheSuccessorAndClosesTheScreen() = runTest {
        val repository = Groups()
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            viewModel.leave(ANA)
            advanceUntilIdle()
            assertTrue((expectMostRecentItem() as UiState.Content).data.left)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(Triple("g1", ME, ANA)), repository.removed)
    }

    @Test fun aRefusedLeaveKeepsTheErrorCodeForTheScreen() = runTest {
        val repository = Groups().also { it.failure = ApiResult.Failure(ErrorKind.CONFLICT, errorCode = "SUCCESSOR_REQUIRED") }
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            viewModel.leave()
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertEquals("SUCCESSOR_REQUIRED", ui.failure?.errorCode)
            assertFalse(ui.left)
            assertFalse(ui.busy)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun addingAndTransferringGoThroughTheRepository() = runTest {
        val repository = Groups()
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            viewModel.add(setOf("44444444-4444-4444-4444-444444444444"))
            advanceUntilIdle()
            assertEquals(4, (expectMostRecentItem() as UiState.Content).data.members.size)
            viewModel.transfer(ANA)
            advanceUntilIdle()
            assertFalse((expectMostRecentItem() as UiState.Content).data.canManage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NewGroupViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class Repository : CommunicationRepository by FakeCommunicationRepository() {
        val created = mutableListOf<Pair<String, Set<String>>>()
        override suspend fun createGroup(title: String, participantIds: Set<String>): ApiResult<GroupCreated> {
            created += title to participantIds
            return ApiResult.Success(GroupCreated("g9", 2))
        }
    }

    @Test fun trimsTheTitleAndReportsTheCreatedGroup() = runTest {
        val repository = Repository()
        val viewModel = NewGroupViewModel(repository)
        viewModel.create("  Obra  ", setOf(ANA))
        advanceUntilIdle()
        assertEquals(NewGroup.Created("g9", 2), viewModel.state.value)
        assertEquals(listOf("Obra" to setOf(ANA)), repository.created)
        viewModel.consumed()
        assertEquals(NewGroup.Idle, viewModel.state.value)
    }

    @Test fun invalidInputNeverReachesTheServer() = runTest {
        val repository = Repository()
        val viewModel = NewGroupViewModel(repository)
        viewModel.create("   ", setOf(ANA))
        assertTrue((viewModel.state.value as NewGroup.Failed).failure.kind == ErrorKind.VALIDATION)
        viewModel.create("x".repeat(121), setOf(ANA))
        viewModel.create("Obra", emptySet())
        assertTrue(repository.created.isEmpty())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LiveDirectNameTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class FakeRealtime : ChatRealtime {
        override val events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
        override val connected: Flow<Boolean> = MutableStateFlow(true)
        override val presence: Flow<PresenceState> = MutableStateFlow(PresenceState())
        override val typingByConversation: Flow<Map<String, Set<String>>> = MutableStateFlow(emptyMap())
        override suspend fun typing(conversationId: String, started: Boolean) = Unit
    }

    private class Names(val resolved: Map<String, String>) : CommunicationRepository by FakeCommunicationRepository() {
        var asked = 0
        override fun currentUserId() = ME
        override suspend fun conversations(): ApiResult<List<Conversation>> = ApiResult.Success(emptyList())
        override suspend fun participantNames(authIds: Collection<String>): Map<String, String> { asked++; return resolved.filterKeys { it in authIds } }
    }

    @Test fun aDirectConversationThatArrivesLiveGetsTheNameOfTheOtherPerson() = runTest {
        val realtime = FakeRealtime()
        val repository = Names(mapOf(ANA to "Ana Souza"))
        val viewModel = ConversationsViewModel(repository, realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            realtime.events.emit(ChatEvent.ConversationUpdated(ConversationDto("d1", "DIRECT", participantIds = listOf(ME, ANA), lastInteractionAt = "2026-11-02T10:00:00Z", unreadCount = 1)))
            advanceUntilIdle()
            val rows = (expectMostRecentItem() as UiState.Content).data.conversations
            assertEquals(listOf("Ana Souza"), rows.map { it.name })
            assertEquals(listOf("AS"), rows.map { it.initials })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.asked)
    }

    @Test fun whenTheNameCannotBeResolvedTheRowStaysGenericWithoutLooping() = runTest {
        val realtime = FakeRealtime()
        val repository = Names(emptyMap())
        val viewModel = ConversationsViewModel(repository, realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            realtime.events.emit(ChatEvent.ConversationUpdated(ConversationDto("d1", "DIRECT", participantIds = listOf(ME, ANA), lastInteractionAt = "2026-11-02T10:00:00Z", unreadCount = 1)))
            advanceUntilIdle()
            assertEquals(listOf("Conversa"), (expectMostRecentItem() as UiState.Content).data.conversations.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.asked)
    }
}
