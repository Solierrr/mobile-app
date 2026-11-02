package com.project.solaria_mobile.feature.communication.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null,
    val createdAt: String,
    val readAt: String? = null,
)

@Serializable
data class NotificationPageDto(
    val content: List<NotificationDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Serializable data class UnreadCountDto(val count: Long)
@Serializable data class NotifyDeviceRequest(val fcmToken: String, val platform: String = "ANDROID", val appVersion: String? = null)

interface NotifyApi {
    @GET("notify/notifications")
    suspend fun page(@Query("page") page: Int, @Query("size") size: Int = 20, @Query("unreadOnly") unreadOnly: Boolean = false): NotificationPageDto

    @GET("notify/notifications/unread-count")
    suspend fun unreadCount(): UnreadCountDto

    @PATCH("notify/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): NotificationDto

    @PATCH("notify/notifications/read-all")
    suspend fun markAllRead()

    @PUT("notify/devices/{installationId}")
    suspend fun registerDevice(@Path("installationId") id: String, @Body body: NotifyDeviceRequest)

    @DELETE("notify/devices/{installationId}")
    suspend fun removeDevice(@Path("installationId") id: String)
}
