package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

data class MapPoint(val id: String, val title: String, val latitude: Double, val longitude: Double)

@Composable
fun ProjectMap(points: List<MapPoint>, selectedId: String?, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, userLocation: LatLng? = null,
    contentPadding: PaddingValues = PaddingValues(), onLoaded: () -> Unit = {}) {
    val camera = rememberCameraPositionState()
    val context = LocalContext.current
    val colors = AppTheme.colors
    var loaded by remember { mutableStateOf(false) }
    val markerIcons = remember(loaded, colors) {
        if (!loaded) null else listOf(false, true).associateWith { active ->
            val scale = context.resources.displayMetrics.density
            val bitmap = Bitmap.createBitmap((48 * scale).toInt(), (54 * scale).toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (if (active) colors.primary else colors.surface).toArgb() }
            canvas.drawRoundRect(2f, 2f, 46f, 46f, 15f, 15f, paint)
            canvas.drawPath(Path().apply { moveTo(18f, 44f); lineTo(24f, 52f); lineTo(30f, 44f); close() }, paint)
            ContextCompat.getDrawable(context, AppIcons.map_pin)?.mutate()?.let {
                it.setTint((if (active) colors.onAccent else colors.onSurface).toArgb())
                it.setBounds(12, 12, 36, 36)
                it.draw(canvas)
            }
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
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
        mapColorScheme = ComposeMapColorScheme.LIGHT,
        contentPadding = contentPadding,
        uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false),
        onMapLoaded = { loaded = true; onLoaded() }) {
        if (loaded) points.forEach { point -> key(point.id) {
            Marker(state = rememberUpdatedMarkerState(LatLng(point.latitude, point.longitude)),
                icon = markerIcons?.get(point.id == selectedId), anchor = Offset(.5f, .96f),
                title = point.title, onClick = { onSelect(point.id); true })
        } }
        userLocation?.let { Circle(center = it, radius = 15.0,
            fillColor = androidx.compose.ui.graphics.Color(0x334285F4), strokeColor = androidx.compose.ui.graphics.Color(0xFF4285F4), strokeWidth = 2f) }
    }
}
