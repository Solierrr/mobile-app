package com.project.solaria_mobile.feature.home.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.home.domain.CompanyLogo
import com.project.solaria_mobile.feature.home.domain.CompanyLogoId
import com.project.solaria_mobile.feature.home.domain.HomeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeHomeRepository @Inject constructor() : HomeRepository {
    private val companies = listOf(
        CompanyLogo("Google", CompanyLogoId.GOOGLE),
        CompanyLogo("O Swift", CompanyLogoId.SWIFT),
        CompanyLogo("Amazon", CompanyLogoId.AMAZON),
        CompanyLogo("Parceiro", CompanyLogoId.PARTNER),
        CompanyLogo("Solar", CompanyLogoId.SOLAR),
    )

    override suspend fun recentCompanies(): ApiResult<List<CompanyLogo>> = ApiResult.Success(companies)
}
