package com.project.solaria_mobile.feature.discovery

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
internal suspend fun currentLocation(context: Context): Location? = withTimeoutOrNull(15_000) {
    val manager = context.getSystemService(LocationManager::class.java)
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .firstOrNull { manager.isProviderEnabled(it) } ?: return@withTimeoutOrNull null
    suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Legacy Android callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        try { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }
        catch (_: SecurityException) { if (continuation.isActive) continuation.resume(null) }
    }
}
