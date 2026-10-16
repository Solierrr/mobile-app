package com.project.solaria_mobile.feature.project.domain

import kotlinx.coroutines.flow.StateFlow

interface ProjectRepository {
    val projects: StateFlow<List<ProjectSummary>>
}
