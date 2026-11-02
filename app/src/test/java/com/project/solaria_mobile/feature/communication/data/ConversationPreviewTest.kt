package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.ApiErrorParser
import com.project.solaria_mobile.core.network.CredentialProvider
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationPreviewTest {
    private val token = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"eu"}""".toByteArray()) + ".s"

    private class PreviewApi : MessengerApi {
        val limits = mutableListOf<Int>()
        var listed = listOf(ConversationDto("c1", title = "Ana"), ConversationDto("c2", title = "Equipe"), ConversationDto("c3", title = "Sem rede"))
        override suspend fun conversations(archived: Boolean) = listed
        override suspend fun conversation(id: String) = ConversationDto(id)
        override suspend fun messages(conversationId: String, limit: Int, sinceSequence: Int?, beforeSequence: Int?): List<MessageDto> {
            limits += limit
            return when (conversationId) {
                "c1" -> listOf(MessageDto("m1", "c1", senderId = "outra", content = "Até\namanhã"))
                "c2" -> listOf(MessageDto("m2", "c2", senderId = "eu", content = "Combinado"))
                else -> throw IOException("sem rede")
            }
        }
        override suspend fun updates(since: Long?, limit: Int) = MessageUpdatesDto()
        override suspend fun direct(body: DirectConversationRequest) = ConversationDto("direct")
        override suspend fun send(body: SendMessageRequest) = MessageDto("m", body.conversationId, content = body.content)
        override suspend fun editMessage(id: String, body: EditMessageRequest) = MessageDto(id, "c1", content = body.content, edited = true)
        override suspend fun deleteMessage(id: String) = MessageDto(id, "c1", deleted = true)
        override suspend fun conversationState(id: String, body: ConversationStatePatch) = ConversationStateDto(id)
    }

    @Test fun previewShowsOnlyTheLatestMessageOfEachConversation() = runTest {
        val api = PreviewApi()
        val repository = RemoteCommunicationRepository(api, ApiCallExecutor(ApiErrorParser(Json)), object : CredentialProvider {
            override fun accessToken() = token
        }, ParticipantNames { null })

        val conversations = (repository.conversations() as ApiResult.Success).data

        assertEquals(listOf("Até amanhã", "Você: Combinado", ""), conversations.map { it.preview })
        assertEquals(listOf(1, 1, 1), api.limits)
    }

    @Test fun currentContractUsesListedPreviewAndUnreadWithoutExtraCalls() = runTest {
        val api = PreviewApi()
        api.listed = listOf(
            ConversationDto("c1", title = "Ana", unreadCount = 3, lastMessage = MessagePreviewDto("m1", "outra", "Até\namanhã")),
            ConversationDto("c2", title = "Equipe", unreadCount = 0, lastMessage = MessagePreviewDto("m2", "eu", "segredo", deleted = true)),
            ConversationDto("c3", title = "Nova", unreadCount = 0),
        )
        val repository = RemoteCommunicationRepository(api, ApiCallExecutor(ApiErrorParser(Json)), object : CredentialProvider {
            override fun accessToken() = token
        }, ParticipantNames { null })

        val conversations = (repository.conversations() as ApiResult.Success).data

        assertEquals(listOf("Até amanhã", "Mensagem excluída", ""), conversations.map { it.preview })
        assertEquals(listOf(3, 0, 0), conversations.map { it.unread })
        assertEquals(emptyList<Int>(), api.limits)
    }
}
