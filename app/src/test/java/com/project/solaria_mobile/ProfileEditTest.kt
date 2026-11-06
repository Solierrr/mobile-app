package com.project.solaria_mobile

import androidx.lifecycle.SavedStateHandle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.profile.ProfileEdit
import com.project.solaria_mobile.feature.profile.ProfileOperation
import com.project.solaria_mobile.feature.profile.ProfileViewModel
import com.project.solaria_mobile.feature.profile.data.FakeProfileRepository
import com.project.solaria_mobile.feature.profile.domain.ProfilePhotoType
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.project.data.FakeProjectRepository
import com.project.solaria_mobile.feature.profile.message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private class EditingRepository(private val failure: ErrorKind? = null) : ProfileRepository by FakeProfileRepository() {
        val usernames = mutableListOf<String>()
        val uploads = mutableListOf<Pair<String, ProfilePhotoType>>()
        override suspend fun changeUsername(username: String): ApiResult<Unit> {
            if (failure != null) return ApiResult.Failure(failure)
            usernames += username
            return ApiResult.Success(Unit)
        }
        override suspend fun uploadOwnPhoto(userId: String, type: ProfilePhotoType, image: ByteArray, mime: String): ApiResult<Unit> {
            uploads += userId to type
            return ApiResult.Success(Unit)
        }
    }

    private fun own(repository: ProfileRepository) = ProfileViewModel(SavedStateHandle(), repository, FakeProjectRepository())

    @Test fun invalidUsernameIsRefusedWithoutCallingTheServer() = runTest {
        val repository = EditingRepository()
        val model = own(repository)
        advanceUntilIdle()

        model.changeUsername("Nome Com Espaço")
        advanceUntilIdle()

        assertTrue(repository.usernames.isEmpty())
        val failed = model.edit.value as ProfileEdit.Failed
        assertEquals("Use de 3 a 30 caracteres: letras minúsculas, números ou sublinhado. O nome pode já estar em uso.", failed.message(TestStrings.resources))
    }

    @Test fun validUsernameIsSavedOnTheServer() = runTest {
        val repository = EditingRepository()
        val model = own(repository)
        advanceUntilIdle()

        model.changeUsername("ana_lima")
        advanceUntilIdle()

        assertEquals(listOf("ana_lima"), repository.usernames)
        assertEquals(ProfileEdit.Done(ProfileOperation.USERNAME), model.edit.value)
    }

    @Test fun takenUsernameIsNotTreatedAsSuccess() = runTest {
        val model = own(EditingRepository(failure = ErrorKind.CONFLICT))
        advanceUntilIdle()

        model.changeUsername("ana_lima")
        advanceUntilIdle()

        assertEquals(ProfileOperation.USERNAME, (model.edit.value as ProfileEdit.Failed).operation)
    }

    @Test fun photoGoesToTheOwnAccountOnly() = runTest {
        val repository = EditingRepository()
        val model = own(repository)
        advanceUntilIdle()

        model.uploadPhoto(ProfilePhotoType.BANNER, byteArrayOf(1), "image/png")
        advanceUntilIdle()

        assertEquals(listOf("me" to ProfilePhotoType.BANNER), repository.uploads)
    }

    @Test fun anotherPersonsProfileNeverSendsPhotos() = runTest {
        val repository = EditingRepository()
        val model = ProfileViewModel(SavedStateHandle(mapOf("id" to "u1")), repository, FakeProjectRepository())
        advanceUntilIdle()

        model.uploadPhoto(ProfilePhotoType.PROFILE, byteArrayOf(1), "image/png")
        advanceUntilIdle()

        assertTrue(repository.uploads.isEmpty())
    }
}
