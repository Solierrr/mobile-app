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
    val authId: String? = null,
)

data class Connection(val id: String, val initials: String, val name: String, val handle: String, val authId: String? = null)
