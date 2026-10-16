package com.project.solaria_mobile.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable data object Splash : AppDestination
    @Serializable data object Home : AppDestination
    @Serializable data object Projects : AppDestination
    @Serializable data class Project(val id: String) : AppDestination
    @Serializable data object Chats : AppDestination
    @Serializable data class Chat(val id: String) : AppDestination
    @Serializable data object Calls : AppDestination
    @Serializable data class Call(val id: String) : AppDestination
    @Serializable data object Schedule : AppDestination
    @Serializable data object Map : AppDestination
    @Serializable data object ProfileSelf : AppDestination
    @Serializable data class ProfileOther(val id: String) : AppDestination
    @Serializable data object Connections : AppDestination
    @Serializable data object Notifications : AppDestination
    @Serializable data object Enterprise : AppDestination
    @Serializable data object Settings : AppDestination
    @Serializable data object Diagnostics : AppDestination
}
