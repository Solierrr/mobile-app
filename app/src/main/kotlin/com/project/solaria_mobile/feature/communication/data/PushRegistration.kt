package com.project.solaria_mobile.feature.communication.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.NetworkMonitor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.core.offline.CacheIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class PushRegistration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokens: TokenStore,
    private val mockMode: MockModeStore,
    private val identity: CacheIdentity,
    private val network: NetworkMonitor,
    private val executor: ApiCallExecutor,
    retrofit: RetrofitFactory,
) {
    private val api = retrofit.create(BuildConfig.NOTIFY_BASE_URL).create(NotifyApi::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val revision = MutableStateFlow(0)
    private val mutex = Mutex()
    private var started = false
    @Volatile private var installation: String? = null
    @Volatile private var registeredAccount: String? = null
    @Volatile private var suppressRegistration = false

    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(tokens.tokens, mockMode.enabled, network.isOnline, revision) { _, _, online, _ -> online }
                .collectLatest { online ->
                        if (FirebaseApp.getApps(context).isEmpty()) return@collectLatest
                        val account = identity.current()
                        if (mockMode.isEnabled() || account == null) {
                            FirebaseMessaging.getInstance().isAutoInitEnabled = false
                            context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
                            if (registeredAccount != null && !mockMode.isEnabled()) withTimeoutOrNull(5_000) { safely { FirebaseMessaging.getInstance().deleteToken().await() } }
                            registeredAccount = null
                            return@collectLatest
                        }
                        if (!online || suppressRegistration) return@collectLatest
                        repeat(3) { attempt ->
                            var success = false
                            withTimeoutOrNull(20_000) { safely {
                            val messaging = FirebaseMessaging.getInstance()
                            messaging.isAutoInitEnabled = true
                            val token = messaging.token.await()
                            val id = FirebaseInstallations.getInstance().id.await()
                            mutex.withLock {
                                if (identity.current() != account || mockMode.isEnabled() || suppressRegistration) return@withLock
                                installation = id
                                val result = executor.execute { api.registerDevice(id, NotifyDeviceRequest(token, appVersion = BuildConfig.VERSION_NAME)) }
                                success = result is com.project.solaria_mobile.core.common.ApiResult.Success
                                if (success) registeredAccount = account
                            }
                            } }
                            if (success || identity.current() != account || mockMode.isEnabled() || suppressRegistration) return@collectLatest
                            if (attempt < 2) delay(2_000L * (attempt + 1))
                        }
                }
        }
    }

    fun tokenChanged() { revision.update { it + 1 } }

    suspend fun beforeSignOut() {
        suppressRegistration = true
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseMessaging.getInstance().isAutoInitEnabled = false
        withTimeoutOrNull(5_000) {
            mutex.withLock {
            installation?.let { if (!mockMode.isEnabled()) executor.execute { api.removeDevice(it) } }
            }
        }
        withTimeoutOrNull(5_000) {
            if (!mockMode.isEnabled() && FirebaseApp.getApps(context).isNotEmpty()) safely { FirebaseMessaging.getInstance().deleteToken().await() }
        }
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
        registeredAccount = null
    }

    fun afterSignOut() {
        suppressRegistration = false
        tokenChanged()
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try { block() } catch (error: Exception) { if (error is CancellationException) throw error }
    }
}
