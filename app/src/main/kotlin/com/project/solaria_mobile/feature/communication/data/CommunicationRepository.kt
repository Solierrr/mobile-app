package com.project.solaria_mobile.feature.communication.data

import kotlinx.coroutines.flow.StateFlow

interface CommunicationRepository {
    val conversations: StateFlow<List<Conversation>>
    val messages: StateFlow<List<ChatMessage>>
    val calls: StateFlow<List<ServiceCall>>
    val notifications: StateFlow<List<Pair<String, List<ServiceCall>>>>
    fun sendMessage(text: String)
}
