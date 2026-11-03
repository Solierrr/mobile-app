package com.project.solaria_mobile

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.communication.NewGroup
import com.project.solaria_mobile.feature.communication.NewGroupViewModel
import com.project.solaria_mobile.feature.communication.data.FakeCommunicationRepository
import com.project.solaria_mobile.feature.communication.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewGroupVerificationTest {
    @get:Rule val main = MainDispatcherRule()
    @Test fun uncertainCreationCannotRepeatUntilTheServerListIsVerified() = runTest {
        var creates = 0
        var online = false
        val created = Conversation("g1", "E", "Equipe", "", "", AvatarTone.BLUE, group = true)
        val repository = object : CommunicationRepository by FakeCommunicationRepository() {
            override suspend fun createGroup(title: String, participantIds: Set<String>): ApiResult<GroupCreated> {
                creates++
                return ApiResult.Failure(ErrorKind.SERVER, errorCode = "GROUP_CREATION_UNCERTAIN")
            }
            override suspend fun conversations(): ApiResult<List<Conversation>> =
                if (online) ApiResult.Success(listOf(created)) else ApiResult.Failure(ErrorKind.CONNECTIVITY)
        }
        val model = NewGroupViewModel(repository)
        model.create("Equipe", setOf("ana"))
        advanceUntilIdle()
        assertTrue((model.state.value as NewGroup.Failed).needsVerification)
        model.consumed()
        model.create("Equipe", setOf("ana"))
        advanceUntilIdle()
        assertEquals(1, creates)
        online = true
        model.verify()
        advanceUntilIdle()
        val checked = model.state.value as NewGroup.Failed
        assertFalse(checked.needsVerification)
        assertEquals(listOf(created), checked.checkedConversations)
    }
}
