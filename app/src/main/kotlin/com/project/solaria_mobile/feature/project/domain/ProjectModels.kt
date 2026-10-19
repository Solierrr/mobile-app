package com.project.solaria_mobile.feature.project.domain

data class ProjectSummary(
    val id: String,
    val name: String,
    val place: String,
    val status: String,
    val people: String,
    val companyId: String? = null,
    val companyName: String = "",
    val companyLogoUrl: String? = null,
    val createdAtMillis: Long = 0,
)
