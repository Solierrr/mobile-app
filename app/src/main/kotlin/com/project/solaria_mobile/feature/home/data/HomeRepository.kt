package com.project.solaria_mobile.feature.home.data

import kotlinx.coroutines.flow.StateFlow

interface HomeRepository {
    val recentCompanies: StateFlow<List<CompanyLogo>>
}
