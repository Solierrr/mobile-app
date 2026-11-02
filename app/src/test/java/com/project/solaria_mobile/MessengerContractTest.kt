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
import com.project.solaria_mobile.feature.communication.ChatViewModel
import com.project.solaria_mobile.feature.communication.data.ChatCapabilities
import com.project.solaria_mobile.feature.communication.data.ChatEvent
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.CommunityGroupDto
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.GroupCreationDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.feature.communication.data.ParticipantNames
import com.project.solaria_mobile.feature.communication.data.RemoteCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.testOutbox
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Contratos do api-messenger na ponta do app: o que o servidor manda (e o que não manda) em comunidade, nomes e envio. */
class MessengerContractTest {
    private val json = ApiJson.instance

    @Test fun communityGroupDecodesWithoutStatusLikeTheServer() {
        val group = json.decodeFromString(
            CommunityGroupDto.serializer(),
            """{"id":"g1","title":"Geral","participantIds":["a","b"],"managerUserId":"a","startedAt":"2026-11-01T10:00:00Z","lastInteractionAt":null}""",
        )
        assertEquals(CommunityGroupDto("g1", "Geral", setOf("a", "b")), group)
    }

    @Test fun rejectedParticipantsCarryTheReasonCode() {
        val created = json.decodeFromString(
            GroupCreationDto.serializer(),
            """{"conversation":{"id":"g1","conversationType":"GROUP"},"rejectedParticipants":[{"userId":"u1","reasonCode":"NOT_COMMUNITY_MEMBER"}]}""",
        )
        assertEquals("NOT_COMMUNITY_MEMBER", created.rejectedParticipants.single().reasonCode)
        assertEquals("u1", created.rejectedParticipants.single().userId)
    }

    @Test fun conversationDecodesParticipantIds() {
        val dto = json.decodeFromString(ConversationDto.serializer(), """{"id":"c1","conversationType":"DIRECT","participantIds":["eu","ana"]}""")
        assertEquals(listOf("eu", "ana"), dto.participantIds)
    }

    // ---- nomes -----------------------------------------------------------------------------------------------------

    @Test fun namesAreResolvedOnceAndAFailureOnlyDropsThatPerson() = runTest {
        val calls = mutableListOf<String>()
        val names = ParticipantNames { id -> calls += id; if (id == "falha") null else "Nome $id" }

        assertEquals(mapOf("ana" to "Nome ana"), names.resolve(listOf("ana", "ana", "falha")))
        // O nome encontrado fica em memória; quem falhou é tentado outra vez.
        assertEquals(mapOf("ana" to "Nome ana"), names.resolve(listOf("ana", "falha")))
        assertEquals(listOf("ana", "falha", "falha"), calls)
    }

    private val token = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"eu"}""".toByteArray()) + ".s"
    private val credentials = object : CredentialProvider { override fun accessToken() = token }
    private val executor = ApiCallExecutor(ApiErrorParser(kotlinx.serialization.json.Json))

    private fun repository(api: MessengerApi, names: ParticipantNames) = RemoteCommunicationRepository(api, executor, credentials, names)

    @Test fun directConversationTakesTheNameOfTheOtherParticipant() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversations(any()) } returns listOf(
            ConversationDto("d1", "DIRECT", participantIds = listOf("eu", "ana"), unreadCount = 0),
            ConversationDto("d2", "DIRECT", participantIds = listOf("bia", "eu"), unreadCount = 0),
            ConversationDto("g1", "GROUP", title = "Equipe", participantIds = listOf("eu", "ana", "bia"), unreadCount = 0),
        )
        val lookedUp = mutableListOf<String>()
        val repository = repository(api, ParticipantNames { id -> lookedUp += id; if (id == "bia") null else "Ana Souza" })

        val list = (repository.conversations() as ApiResult.Success).data

        // Falhou o summary de "bia": a lista vem inteira e essa conversa cai no nome genérico; o grupo mantém o título.
        assertEquals(listOf("Ana Souza", "Conversa", "Equipe"), list.map { it.name })
        assertEquals(listOf("ana", "bia"), lookedUp.sorted())
    }

    @Test fun openedConversationCarriesTheNameOfEveryOtherParticipant() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversation("g1") } returns ConversationDto("g1", "GROUP", title = "Equipe", participantIds = listOf("eu", "ana", "bia"))
        val repository = repository(api, ParticipantNames { id -> if (id == "bia") null else "Nome $id" })

        val conversation = (repository.conversation("g1") as ApiResult.Success).data

        assertEquals("Equipe", conversation.name)
        assertEquals(mapOf("ana" to "Nome ana"), conversation.participantNames)
    }

    // ---- tipo de mensagem em cache ---------------------------------------------------------------------------------

    @Test fun messageTypeComesFromTheCacheAfterTheConversationWasSeen() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversation("g1") } returns ConversationDto("g1", "GROUP", title = "Equipe")
        coEvery { api.send(any()) } answers { MessageDto("m", firstArg<com.project.solaria_mobile.feature.communication.data.SendMessageRequest>().conversationId) }
        val repository = repository(api, ParticipantNames { null })

        repository.conversation("g1")
        repository.sendMessage("g1", "oi")
        repository.sendMessage("g1", "de novo")

        // Uma única leitura da conversa (a de abrir); os dois envios não buscam de novo.
        coVerify(exactly = 1) { api.conversation("g1") }
        coVerify(exactly = 2) { api.send(match { it.messageType == "USER_TO_GROUP" }) }
    }

    @Test fun messageTypeIsFetchedOnlyWhenUnknownAndThenReused() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversation("d1") } returns ConversationDto("d1", "DIRECT", participantIds = listOf("eu", "ana"))
        coEvery { api.send(any()) } returns MessageDto("m", "d1")
        val repository = repository(api, ParticipantNames { null })

        repository.sendMessage("d1", "um")
        repository.sendMessage("d1", "dois")

        coVerify(exactly = 1) { api.conversation("d1") }
        coVerify(exactly = 2) { api.send(match { it.messageType == "USER_TO_USER" }) }
    }

    @Test fun listingTheConversationsAlsoFillsTheMessageTypeCache() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversations(any()) } returns listOf(ConversationDto("a1", "CHAT_BOT", unreadCount = 0))
        coEvery { api.send(any()) } returns MessageDto("m", "a1")
        val repository = repository(api, ParticipantNames { null })

        repository.conversations()
        repository.sendMessage("a1", "oi")

        coVerify(exactly = 0) { api.conversation(any()) }
        coVerify { api.send(match { it.messageType == "USER_TO_CHATBOT" }) }
    }

    @Test fun failingToLearnTheTypeFailsTheSendWithoutCaching() = runTest {
        val api = mockk<MessengerApi>()
        coEvery { api.conversation("x") } throws IOException("sem rede")
        val repository = repository(api, ParticipantNames { null })

        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), repository.sendMessage("x", "oi"))
        coVerify(exactly = 0) { api.send(any()) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatNamesAndRemovalTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class FakeRealtime : ChatRealtime {
        val events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
        override val connected: Flow<Boolean> = flowOf(true)
        override fun conversationEvents(conversationId: String): Flow<ChatEvent> = events
        override suspend fun typing(conversationId: String, started: Boolean) = Unit
    }

    private class GroupRepository : CommunicationRepository by FakeCommunicationRepository() {
        override suspend fun conversation(id: String): ApiResult<Conversation> =
            ApiResult.Success(Conversation(id, "EQ", "Equipe", "", "", AvatarTone.BLUE, group = true, participantNames = mapOf("ana" to "Ana Souza")))
    }

    private fun viewModel(realtime: ChatRealtime): ChatViewModel {
        val repository = GroupRepository()
        return ChatViewModel(SavedStateHandle(mapOf("id" to "g1")), repository, testOutbox(repository), realtime, ChatCapabilities { false })
    }

    @Test fun senderAndTypingNamesComeFromTheResolvedParticipants() = runTest {
        val realtime = FakeRealtime()
        val viewModel = viewModel(realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            realtime.events.emit(ChatEvent.MessageCreated(MessageDto("live-1", "g1", "ana", "oi", "2026-10-20T09:00:00Z", 50)))
            realtime.events.emit(ChatEvent.Typing("ana", true))
            runCurrent()
            val state = (expectMostRecentItem() as UiState.Content).data
            assertEquals("Ana Souza", state.messages.single { it.id == "live-1" }.senderName)
            assertEquals(listOf("Ana Souza"), state.typingNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun losingTheParticipationLeavesTheChatWithoutArchivingIt() = runTest {
        val realtime = FakeRealtime()
        val viewModel = viewModel(realtime)
        viewModel.uiState.test {
            advanceUntilIdle()
            val before = (expectMostRecentItem() as UiState.Content).data
            assertFalse(before.removed)
            realtime.events.emit(ChatEvent.ConversationRemoved("g1"))
            advanceUntilIdle()
            val after = (expectMostRecentItem() as UiState.Content).data
            assertTrue(after.removed)
            assertFalse(after.archived)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
