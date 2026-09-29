package com.project.solaria_mobile.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable data object Splash : AppDestination
    @Serializable data object Home : AppDestination
    @Serializable data object Projects : AppDestination
    @Serializable data object Project : AppDestination
    @Serializable data object Chats : AppDestination
    @Serializable data object Chat : AppDestination
    @Serializable data object Calls : AppDestination
    @Serializable data object Call : AppDestination
    @Serializable data object Schedule : AppDestination
    @Serializable data object Map : AppDestination
    @Serializable data object ProfileSelf : AppDestination
    @Serializable data object ProfileOther : AppDestination
    @Serializable data object Connections : AppDestination
    @Serializable data object Notifications : AppDestination
    @Serializable data object Enterprise : AppDestination
    @Serializable data object Settings : AppDestination
}
