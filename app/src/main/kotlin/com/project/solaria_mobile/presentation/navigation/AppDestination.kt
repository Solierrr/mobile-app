package com.project.solaria_mobile.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable data object Splash : AppDestination
    @Serializable data object Home : AppDestination
    @Serializable data object Projects : AppDestination
    @Serializable data class Project(val id: String) : AppDestination
    @Serializable data object NewProject : AppDestination
    @Serializable data class EditProject(val id: String) : AppDestination
    @Serializable data object Chats : AppDestination
    @Serializable data object Assistant : AppDestination
    @Serializable data object NewConversation : AppDestination
    @Serializable data class Chat(val id: String) : AppDestination
    @Serializable data object Calls : AppDestination
    @Serializable data class Call(val id: String) : AppDestination
    @Serializable data object Schedule : AppDestination
    @Serializable data class Map(val projectId: String? = null, val assistantSession: String? = null) : AppDestination
    @Serializable data object ProfileSelf : AppDestination
    @Serializable data class ProfileOther(val id: String) : AppDestination
    @Serializable data object Connections : AppDestination
    @Serializable data object Notifications : AppDestination
    @Serializable data class Enterprise(val id: String? = null) : AppDestination
    @Serializable data object Settings : AppDestination
    @Serializable data object Diagnostics : AppDestination
    @Serializable data object Onboarding : AppDestination
}
