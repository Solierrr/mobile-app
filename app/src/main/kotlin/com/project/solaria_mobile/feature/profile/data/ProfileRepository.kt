package com.project.solaria_mobile.feature.profile.data

import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val ownProfile: StateFlow<UserProfile>
    val otherProfile: StateFlow<UserProfile>
    val connections: StateFlow<List<Connection>>
    val connected: StateFlow<Boolean>
    fun toggleConnection()
}
