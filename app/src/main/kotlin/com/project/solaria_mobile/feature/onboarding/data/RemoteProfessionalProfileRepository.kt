package com.project.solaria_mobile.feature.onboarding.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfile
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProfessionalProfileRepository @Inject constructor(
    private val api: ProfessionalProfileApi,
    private val executor: ApiCallExecutor,
) : ProfessionalProfileRepository {
    override suspend fun load(): ApiResult<ProfessionalProfile> = executor.execute { api.profile() }.map { it.toDomain() }

    override suspend fun save(profile: ProfessionalProfile): ApiResult<ProfessionalProfile> =
        executor.execute { api.save(profile.toRequest()) }.map { it.toDomain() }

    override suspend fun complete(): ApiResult<ProfessionalProfile> = executor.execute { api.complete() }.map { it.toDomain() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfessionalProfileModule {
    @Binds abstract fun bindRepository(repository: RemoteProfessionalProfileRepository): ProfessionalProfileRepository
}
