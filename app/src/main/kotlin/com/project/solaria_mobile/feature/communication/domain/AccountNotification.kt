package com.project.solaria_mobile.feature.communication.domain

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult

data class AccountNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val createdAt: String,
    val readAt: String? = null,
)

data class NotificationPage(val items: List<AccountNotification>, val page: Int, val totalPages: Int)

sealed interface NotificationTarget {
    data class Conversation(val id: String) : NotificationTarget
    data class Service(val id: String) : NotificationTarget
    data class Project(val id: String) : NotificationTarget
}

fun AccountNotification.target(): NotificationTarget? {
    fun id(vararg keys: String): String? = keys.firstNotNullOfOrNull { data[it] }
        ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
    return when (type) {
        "NEW_MESSAGE" -> id("chatId", "conversationId")?.let(NotificationTarget::Conversation)
        "SERVICE_SCHEDULED", "SERVICE_STATUS_CHANGED" -> id("technicalServiceId", "serviceId")?.let(NotificationTarget::Service)
        else -> id("projectId")?.let(NotificationTarget::Project)
    }
}

interface NotificationRepository {
    suspend fun page(page: Int, unreadOnly: Boolean): ReadResult<NotificationPage>
    suspend fun unreadCount(): ApiResult<Long>
    suspend fun markRead(id: String): ApiResult<AccountNotification>
    suspend fun markAllRead(): ApiResult<Unit>
}
