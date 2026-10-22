package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

data class MapPoint(val id: String, val title: String, val latitude: Double, val longitude: Double)

@Composable
fun ProjectMap(points: List<MapPoint>, selectedId: String?, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, userLocation: LatLng? = null, onLoaded: () -> Unit = {}) {
    val camera = rememberCameraPositionState()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(points, selectedId, loaded, userLocation) {
        if (!loaded) return@LaunchedEffect
        val selected = points.firstOrNull { it.id == selectedId }
        when {
            userLocation != null -> camera.animate(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            selected != null -> camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(selected.latitude, selected.longitude), 15f))
            points.size == 1 -> camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(points[0].latitude, points[0].longitude), 14f))
            points.size > 1 -> {
                val bounds = LatLngBounds.builder()
                points.forEach { bounds.include(LatLng(it.latitude, it.longitude)) }
                camera.move(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80))
            }
            else -> camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(-14.2, -51.9), 3f))
        }
    }
    GoogleMap(modifier = modifier, cameraPositionState = camera,
        uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false),
        onMapLoaded = { loaded = true; onLoaded() }) {
        points.forEach { point -> key(point.id) {
            Marker(state = rememberUpdatedMarkerState(LatLng(point.latitude, point.longitude)),
                title = point.title, onClick = { onSelect(point.id); true })
        } }
        userLocation?.let { Circle(center = it, radius = 15.0,
            fillColor = androidx.compose.ui.graphics.Color(0x334285F4), strokeColor = androidx.compose.ui.graphics.Color(0xFF4285F4), strokeWidth = 2f) }
    }
}
