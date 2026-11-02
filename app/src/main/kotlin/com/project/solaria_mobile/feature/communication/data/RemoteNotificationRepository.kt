package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.communication.domain.AccountNotification
import com.project.solaria_mobile.feature.communication.domain.NotificationPage
import com.project.solaria_mobile.feature.communication.domain.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.mock.MockModeStore

@Singleton
class NotificationUpdates @Inject constructor() {
    private val mutable = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = mutable.asSharedFlow()
    fun changed() { mutable.tryEmit(Unit) }
}

@Singleton
class RemoteNotificationRepository @Inject constructor(
    retrofit: RetrofitFactory,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val network: NetworkMonitor,
    private val mockMode: MockModeStore,
) : NotificationRepository {
    private val api = retrofit.create(BuildConfig.NOTIFY_BASE_URL).create(NotifyApi::class.java)

    override suspend fun page(page: Int, unreadOnly: Boolean): ReadResult<NotificationPage> =
        reader.readResult("notify:$unreadOnly:$page", NotificationPageDto.serializer()) { api.page(page, unreadOnly = unreadOnly) }
            .map { response -> NotificationPage(response.content.map(NotificationDto::toNotification), response.page, response.totalPages) }

    override suspend fun unreadCount(): ApiResult<Long> = online { api.unreadCount().count }

    override suspend fun markRead(id: String): ApiResult<AccountNotification> =
        online {
            val dto = api.markRead(id)
            reader.invalidatePrefix("notify:")
            dto.toNotification()
        }

    override suspend fun markAllRead(): ApiResult<Unit> = online {
        api.markAllRead()
        reader.invalidatePrefix("notify:")
    }

    private suspend fun <T> online(block: suspend () -> T): ApiResult<T> =
        if (mockMode.isEnabled() || network.isOnline.first()) executor.execute(block) else ApiResult.Failure(ErrorKind.CONNECTIVITY)
}

internal fun NotificationDto.toNotification() = AccountNotification(id, type, title, body, data.orEmpty(), createdAt, readAt)

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationRepositoryModule {
    @Binds abstract fun repository(implementation: RemoteNotificationRepository): NotificationRepository
}
