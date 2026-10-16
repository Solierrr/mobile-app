package com.project.solaria_mobile.feature.home.domain

import kotlinx.coroutines.flow.StateFlow

interface HomeRepository {
    val recentCompanies: StateFlow<List<CompanyLogo>>
}
