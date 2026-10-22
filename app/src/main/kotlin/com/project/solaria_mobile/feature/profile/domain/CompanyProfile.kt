package com.project.solaria_mobile.feature.profile.domain

import com.project.solaria_mobile.feature.project.domain.ProjectSummary

data class CompanyProfile(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val projects: List<ProjectSummary>,
    val detailsAvailable: Boolean = true,
)

fun companyFromProjects(id: String, projects: List<ProjectSummary>): CompanyProfile? {
    val linked = projects.filter { it.companyId == id }
    val company = linked.firstOrNull { it.companyName.isNotBlank() } ?: return null
    return CompanyProfile(id, company.companyName, linked.firstNotNullOfOrNull { it.companyLogoUrl }, linked.sortedByDescending { it.createdAtMillis })
}
