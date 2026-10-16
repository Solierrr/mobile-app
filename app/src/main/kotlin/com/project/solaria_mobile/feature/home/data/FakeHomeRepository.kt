package com.project.solaria_mobile.feature.home.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import com.project.solaria_mobile.feature.home.domain.CompanyLogo
import com.project.solaria_mobile.feature.home.domain.CompanyLogoId
import com.project.solaria_mobile.feature.home.domain.HomeRepository

@Singleton
class FakeHomeRepository @Inject constructor() : HomeRepository {
    override val recentCompanies = MutableStateFlow(
        listOf(
            CompanyLogo("Google", CompanyLogoId.GOOGLE),
            CompanyLogo("O Swift", CompanyLogoId.SWIFT),
            CompanyLogo("Amazon", CompanyLogoId.AMAZON),
            CompanyLogo("Parceiro", CompanyLogoId.PARTNER),
            CompanyLogo("Solar", CompanyLogoId.SOLAR),
        )
    )
}
