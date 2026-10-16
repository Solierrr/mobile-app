package com.project.solaria_mobile.feature.profile.domain


data class UserProfile(
    val initials: String,
    val name: String,
    val handle: String,
    val state: String,
    val occupation: String,
    val phone: String,
)

data class Connection(val initials: String, val name: String, val handle: String)
