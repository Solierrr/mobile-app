package com.project.solaria_mobile.feature.home.domain

import com.project.solaria_mobile.core.common.ApiResult

interface HomeRepository {
    suspend fun recentCompanies(): ApiResult<List<CompanyLogo>>
}
