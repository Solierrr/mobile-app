package com.project.solaria_mobile.feature.auth.domain

sealed interface LoginDestination {
    data class Browser(val url: String) : LoginDestination
    data object Authenticated : LoginDestination
}
