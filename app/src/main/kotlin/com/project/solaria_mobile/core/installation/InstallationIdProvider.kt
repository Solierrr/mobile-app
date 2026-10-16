package com.project.solaria_mobile.core.installation

import com.google.firebase.installations.FirebaseInstallations
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface InstallationIdProvider {
    suspend fun installationId(): ApiResult<String>
}

@Singleton
class FirebaseInstallationIdProvider @Inject constructor() : InstallationIdProvider {
    override suspend fun installationId(): ApiResult<String> = suspendCancellableCoroutine { continuation ->
        try {
            FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                val id = task.result
                if (task.isSuccessful && !id.isNullOrBlank()) {
                    continuation.resume(ApiResult.Success(id))
                } else {
                    continuation.resume(ApiResult.Failure(ErrorKind.UNKNOWN))
                }
            }
        } catch (_: IllegalStateException) {
            continuation.resume(ApiResult.Failure(ErrorKind.UNKNOWN))
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InstallationModule {
    @Binds abstract fun bindProvider(provider: FirebaseInstallationIdProvider): InstallationIdProvider
}
