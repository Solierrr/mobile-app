package com.project.solaria_mobile.feature.profile.domain

data class UserProfile(
    val id: String,
    val initials: String,
    val name: String,
    val handle: String,
    val state: String,
    val occupation: String,
    val phone: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val companyId: String? = null,
    val connected: Boolean = false,
)

data class Connection(val id: String, val initials: String, val name: String, val handle: String, val avatarUrl: String? = null)

data class PeoplePage(val items: List<Connection>, val hasMore: Boolean = false)
