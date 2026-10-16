package com.project.solaria_mobile.feature.project.data

import kotlinx.coroutines.flow.StateFlow

interface ProjectRepository {
    val projects: StateFlow<List<ProjectSummary>>
}
