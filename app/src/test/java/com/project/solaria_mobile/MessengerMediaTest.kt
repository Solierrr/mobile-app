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
import com.project.solaria_mobile.feature.communication.NpsAnswers
import com.project.solaria_mobile.feature.communication.data.AttachmentDto
import com.project.solaria_mobile.feature.communication.data.ChatCapabilities
import com.project.solaria_mobile.feature.communication.data.ConversationDto
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.MediaGateway
import com.project.solaria_mobile.feature.communication.data.MediaUploadDto
import com.project.solaria_mobile.feature.communication.data.MediaUploadRequest
import com.project.solaria_mobile.feature.communication.data.MediaUploadTarget
import com.project.solaria_mobile.feature.communication.data.MediaUploadTargetDto
import com.project.solaria_mobile.feature.communication.data.MessageDto
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.feature.communication.data.NpsRequest
import com.project.solaria_mobile.feature.communication.data.ParticipantNames
import com.project.solaria_mobile.feature.communication.data.RemoteCommunicationRepository
import com.project.solaria_mobile.feature.communication.data.testOutbox
import com.project.solaria_mobile.feature.communication.data.toAttachment
import com.project.solaria_mobile.feature.communication.data.toRequestDto
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.CommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private const val ME_ID = "a1111111-1111-1111-1111-111111111111"
private val sessionToken = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$ME_ID"}""".toByteArray()) + ".s"

private class FakeGateway(var accepts: Boolean = true) : MediaGateway {
    val uploads = mutableListOf<Triple<MediaUploadTarget, String, Long>>()
    var saved: String? = "file:///app/chat-media/m1.pdf"
    override suspend fun upload(target: MediaUploadTarget, uri: String, mimeType: String, sizeBytes: Long): Boolean {
        uploads += Triple(target, uri, sizeBytes)
        return accepts
    }
    override suspend fun save(mediaId: String, name: String?, body: ResponseBody): String? = saved
}

/** Mídia pelo bucket: pedir a permissão, subir os bytes, citar o `mediaId`, e baixar o que chega. */
class MessengerMediaRepositoryTest {
    private val gateway = FakeGateway()
    private val api = mockk<MessengerApi>()

    private fun repository() = RemoteCommunicationRepository(
        api, ApiCallExecutor(ApiErrorParser(ApiJson.instance)), object : CredentialProvider { override fun accessToken() = sessionToken },
        ParticipantNames { null }, gateway,
    )

    private fun stubConversation() {
        coEvery { api.conversation("c1") } returns ConversationDto("c1", "GROUP", title = "Equipe")
        coEvery { api.mediaUpload(any()) } returns MediaUploadDto("media-1", MediaUploadTargetDto("https://bucket/x", "PUT", mapOf("Content-Type" to "image/png")), maxBytes = 10_000_000)
    }

    @Test fun imageDimensionsSurviveSendingAndReceiving() {
        val source = MessageAttachment.Image("file:///photo.jpg", sizeBytes = 1024, width = 640, height = 480)
        val dto = source.toRequestDto("media-1")
        assertEquals(640, dto.width)
        assertEquals(480, dto.height)
        val received = dto.toAttachment() as MessageAttachment.Image
        assertEquals(640, received.width)
        assertEquals(480, received.height)
    }

    @Test fun availableMediaPreservesTheOpaqueCursor() = runTest {
        coEvery { api.availableMedia("c1", 30, "page:2") } returns
            com.project.solaria_mobile.feature.communication.data.MediaPageDto(
                listOf(com.project.solaria_mobile.feature.communication.data.MediaItemDto("m1", "c1", attachment = AttachmentDto("IMAGE", mediaId = "photo"))),
                "page:3",
            )
        val page = (repository().availableMedia("c1", 30, "page:2") as ApiResult.Success).data
        assertEquals("page:3", page.nextCursor)
        assertEquals("photo", (page.items.single().attachment as MessageAttachment.Image).mediaId)
    }

    @Test fun anImageIsUploadedBeforeTheMessageCitesItsMediaId() = runTest {
        stubConversation()
        val sent = slot<com.project.solaria_mobile.feature.communication.data.SendMessageRequest>()
        coEvery { api.send(capture(sent)) } answers {
            MessageDto("m1", "c1", ME_ID, "", attachment = AttachmentDto("IMAGE", mediaId = "media-1", mimeType = "image/png", sizeBytes = 2048, downloadPath = "/messaging/media/media-1/content"))
        }

        val result = repository().sendAttachment("c1", MessageAttachment.Image("file:///cache/a.png", sizeBytes = 2048, mimeType = "image/png"))

        val message = (result as ApiResult.Success).data
        assertEquals(listOf(Triple(MediaUploadTarget("https://bucket/x", "PUT", mapOf("Content-Type" to "image/png")), "file:///cache/a.png", 2048L)), gateway.uploads)
        coVerify { api.mediaUpload(MediaUploadRequest("c1", "IMAGE", "image/png", 2048)) }
        val attachment = sent.captured.attachment!!
        assertEquals("IMAGE", attachment.type)
        assertEquals("media-1", attachment.mediaId)
        assertNull(attachment.url)
        assertEquals("USER_TO_GROUP", sent.captured.messageType)
        // Quem enviou continua vendo o arquivo que tem no aparelho.
        val shown = message.attachment as MessageAttachment.Image
        assertEquals("file:///cache/a.png", shown.url)
        assertEquals("media-1", shown.mediaId)
    }

    @Test fun audioAndVideoTravelWithTheirOwnType() = runTest {
        stubConversation()
        coEvery { api.send(any()) } returns MessageDto("m1", "c1", ME_ID, "")
        val repository = repository()
        repository.sendAttachment("c1", MessageAttachment.File("voz.m4a", 1000, "audio/mp4", "content://x/1"))
        repository.sendAttachment("c1", MessageAttachment.File("video.mp4", 1000, "video/mp4", "content://x/2"))
        repository.sendAttachment("c1", MessageAttachment.File("a.pdf", 1000, "application/pdf", "content://x/3"))
        coVerify { api.mediaUpload(MediaUploadRequest("c1", "AUDIO", "audio/mp4", 1000)) }
        coVerify { api.mediaUpload(MediaUploadRequest("c1", "VIDEO", "video/mp4", 1000)) }
        coVerify { api.mediaUpload(MediaUploadRequest("c1", "FILE", "application/pdf", 1000)) }
    }

    @Test fun aLocationNeverTouchesTheBucket() = runTest {
        stubConversation()
        val sent = slot<com.project.solaria_mobile.feature.communication.data.SendMessageRequest>()
        coEvery { api.send(capture(sent)) } returns MessageDto("m1", "c1", ME_ID, "")
        repository().sendAttachment("c1", MessageAttachment.Location(-23.5, -46.6, "Obra"))
        assertTrue(gateway.uploads.isEmpty())
        coVerify(exactly = 0) { api.mediaUpload(any()) }
        assertEquals("LOCATION", sent.captured.attachment?.type)
        assertEquals(-23.5, sent.captured.attachment?.latitude)
    }

    @Test fun overTheLimitOrWithoutSizeIsRefusedBeforeAnyCall() = runTest {
        stubConversation()
        val repository = repository()
        val tooBig = repository.sendAttachment("c1", MessageAttachment.Image("file:///a.jpg", sizeBytes = 11L * 1024 * 1024))
        val noSize = repository.sendAttachment("c1", MessageAttachment.File("a.pdf", 0, "application/pdf", "content://x"))
        val videoOk = 64L * 1024 * 1024
        assertEquals(ApiResult.Failure(ErrorKind.VALIDATION), tooBig)
        assertEquals(ApiResult.Failure(ErrorKind.VALIDATION), noSize)
        coVerify(exactly = 0) { api.mediaUpload(any()) }
        coEvery { api.send(any()) } returns MessageDto("m1", "c1", ME_ID, "")
        assertTrue(repository.sendAttachment("c1", MessageAttachment.File("v.mp4", videoOk, "video/mp4", "content://v")) is ApiResult.Success)
    }

    @Test fun aFailedUploadDoesNotSendTheMessage() = runTest {
        stubConversation()
        gateway.accepts = false
        val result = repository().sendAttachment("c1", MessageAttachment.Image("file:///a.png", sizeBytes = 10, mimeType = "image/png"))
        assertEquals(ApiResult.Failure(ErrorKind.CONNECTIVITY), result)
        coVerify(exactly = 0) { api.send(any()) }
    }

    @Test fun downloadingSavesTheBodyOnTheDevice() = runTest {
        coEvery { api.mediaContent("media-1") } returns "pdf".toResponseBody("application/pdf".toMediaType())
        assertEquals(ApiResult.Success("file:///app/chat-media/m1.pdf"), repository().downloadMedia("media-1", "contrato.pdf"))
    }

    @Test fun anExpiredMediaCarriesItsErrorCode() = runTest {
        coEvery { api.mediaContent("old") } throws HttpException(Response.error<Any>(410, """{"errorCode":"MEDIA_EXPIRED"}""".toResponseBody("application/json".toMediaType())))
        val failure = repository().downloadMedia("old", null) as ApiResult.Failure
        assertEquals("MEDIA_EXPIRED", failure.errorCode)
    }

    @Test fun npsGoesToTheServerAndDuplicateKeepsItsCode() = runTest {
        coEvery { api.nps(NpsRequest("c1", 9)) } returns JsonObject(emptyMap())
        coEvery { api.nps(NpsRequest("c2", 3)) } throws HttpException(Response.error<Any>(409, """{"errorCode":"DUPLICATE_RESOURCE"}""".toResponseBody("application/json".toMediaType())))
        val repository = repository()
        assertEquals(ApiResult.Success(Unit), repository.submitNps("c1", 9))
        assertEquals("DUPLICATE_RESOURCE", (repository.submitNps("c2", 3) as ApiResult.Failure).errorCode)
    }

    @Test fun serverAttachmentsBecomeMediaReferences() {
        val image = AttachmentDto("IMAGE", mediaId = "m1", mimeType = "image/png", sizeBytes = 10, expiresAt = "2026-11-09T10:00:00Z").toAttachment() as MessageAttachment.Image
        assertEquals("", image.url)
        assertEquals("m1", image.mediaId)
        assertEquals(Instant.parse("2026-11-09T10:00:00Z"), image.expiresAt)
        val audio = AttachmentDto("AUDIO", mediaId = "m2", name = "voz.m4a", mimeType = "audio/mp4", sizeBytes = 99).toAttachment() as MessageAttachment.File
        assertEquals("audio/mp4", audio.mimeType)
        assertEquals("m2", audio.mediaId)
        assertNull(AttachmentDto("IMAGE").toAttachment())
        // O modo de demonstração antigo, só com URL, continua aparecendo.
        assertEquals("mock-asset:x", (AttachmentDto("IMAGE", url = "mock-asset:x").toAttachment() as MessageAttachment.Image).url)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMediaViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class Media(val history: List<ChatMessage>) : CommunicationRepository by FakeCommunicationRepository() {
        val downloads = mutableListOf<String>()
        var outcome: ApiResult<String> = ApiResult.Success("file:///app/chat-media/downloaded")
        val nps = mutableListOf<Pair<String, Int>>()
        var npsOutcome: ApiResult<Unit> = ApiResult.Success(Unit)

        override suspend fun conversation(id: String): ApiResult<Conversation> = ApiResult.Success(Conversation(id, "A", "Assistente", "", "", AvatarTone.ORANGE, assistant = id == "bot"))
        override suspend fun messages(conversationId: String): ApiResult<List<ChatMessage>> = ApiResult.Success(history)
        override suspend fun downloadMedia(mediaId: String, name: String?): ApiResult<String> { downloads += mediaId; return outcome }
        override suspend fun submitNps(conversationId: String, score: Int): ApiResult<Unit> { nps += conversationId to score; return npsOutcome }
    }

    private fun photo(id: String, mediaId: String, expires: Instant? = null) =
        ChatMessage("", false, "10:00", id = id, sequence = id.removePrefix("m").toInt(), attachment = MessageAttachment.Image("", mediaId, expiresAt = expires))

    private fun viewModel(repository: Media, conversation: String = "c1"): ChatViewModel =
        ChatViewModel(SavedStateHandle(mapOf("id" to conversation)), repository, testOutbox(repository), com.project.solaria_mobile.feature.communication.data.NoChatRealtime, ChatCapabilities { true })

    @Test fun aReceivedPhotoDownloadsByItselfAndShowsTheLocalFile() = runTest {
        val repository = Media(listOf(photo("m1", "media-1")))
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            val image = (expectMostRecentItem() as UiState.Content).data.messages.single().attachment as MessageAttachment.Image
            assertEquals("file:///app/chat-media/downloaded", image.url)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("media-1"), repository.downloads)
    }

    @Test fun anExpiredPhotoIsMarkedUnavailableWithoutCallingTheServer() = runTest {
        val repository = Media(listOf(photo("m1", "old", expires = Instant.now().minusSeconds(60))))
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertEquals(setOf("old"), ui.mediaUnavailable)
            assertEquals("", (ui.messages.single().attachment as MessageAttachment.Image).url)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.downloads.isEmpty())
    }

    @Test fun anExpiredAnswerFromTheServerMarksTheMediaUnavailableOnce() = runTest {
        val repository = Media(listOf(photo("m1", "media-1"))).also { it.outcome = ApiResult.Failure(ErrorKind.UNKNOWN, errorCode = "MEDIA_EXPIRED") }
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(setOf("media-1"), (expectMostRecentItem() as UiState.Content).data.mediaUnavailable)
            viewModel.download("media-1", null)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("media-1"), repository.downloads)
    }

    @Test fun aNetworkFailureLeavesTheTapToRetry() = runTest {
        val repository = Media(listOf(photo("m1", "media-1"))).also { it.outcome = ApiResult.Failure(ErrorKind.CONNECTIVITY) }
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            val ui = (expectMostRecentItem() as UiState.Content).data
            assertTrue(ui.mediaUnavailable.isEmpty() && ui.mediaLoading.isEmpty())
            repository.outcome = ApiResult.Success("file:///app/chat-media/again")
            viewModel.download("media-1", null)
            advanceUntilIdle()
            assertEquals("file:///app/chat-media/again", ((expectMostRecentItem() as UiState.Content).data.messages.single().attachment as MessageAttachment.Image).url)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun aDocumentWaitsForTheTapInsteadOfDownloading() = runTest {
        val file = ChatMessage("", false, "10:00", id = "m1", sequence = 1, attachment = MessageAttachment.File("c.pdf", 10, "application/pdf", "", "media-9"))
        val repository = Media(listOf(file))
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals("", ((expectMostRecentItem() as UiState.Content).data.messages.single().attachment as MessageAttachment.File).url)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.downloads.isEmpty())
    }

    @Test fun anAnsweredAssistantSessionAsksForTheNpsOnceAndRemembersTheAnswer() = runTest {
        NpsAnswers.clear()
        val reply = ChatMessage("oi", false, "10:00", id = "m1", sequence = 1)
        val repository = Media(listOf(reply))
        val viewModel = viewModel(repository, conversation = "bot")
        viewModel.uiState.test {
            advanceUntilIdle()
            assertTrue((expectMostRecentItem() as UiState.Content).data.npsPending)
            viewModel.submitNps(8)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("bot" to 8), repository.nps)
        assertTrue("bot" in NpsAnswers)
    }

    @Test fun aDuplicateAnswerAlsoStopsAskingButOtherErrorsDoNot() = runTest {
        NpsAnswers.clear()
        val repository = Media(emptyList()).also { it.npsOutcome = ApiResult.Failure(ErrorKind.CONFLICT, errorCode = "DUPLICATE_RESOURCE") }
        val viewModel = viewModel(repository, conversation = "bot")
        viewModel.submitNps(5)
        advanceUntilIdle()
        assertTrue("bot" in NpsAnswers)
        NpsAnswers.clear()
        repository.npsOutcome = ApiResult.Failure(ErrorKind.CONNECTIVITY)
        viewModel.submitNps(5)
        advanceUntilIdle()
        assertFalse("bot" in NpsAnswers)
    }

    @Test fun aPlainConversationNeverAsksForTheNps() = runTest {
        val repository = Media(listOf(ChatMessage("oi", false, "10:00", id = "m1", sequence = 1)))
        val viewModel = viewModel(repository, conversation = "c1")
        viewModel.uiState.test {
            advanceUntilIdle()
            assertFalse((expectMostRecentItem() as UiState.Content).data.npsPending)
            cancelAndIgnoreRemainingEvents()
        }
        assertNotNull(repository)
    }
}
