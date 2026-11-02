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
import com.google.android.gms.maps.GoogleMapOptions
import com.project.solaria_mobile.BuildConfig
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

// Os prédios em 3D só aparecem em zoom alto; a inclinação os revela ao selecionar um projeto.
private const val SELECTED_ZOOM = 17f

/** Um passo além da visão do projeto, onde os prédios em 3D aparecem: ninguém se perde aproximando demais. */
private const val MAX_ZOOM = 18f

/** Um passo aquém da visão do projeto: o mais que dá para afastar com um projeto selecionado. */
private const val MIN_ZOOM_SELECTED = 16f
private const val DEFAULT_MIN_ZOOM = 3f
private const val SELECTED_TILT = 45f
private const val GROUND_RADIUS = 6.0
private const val GROUND_RADIUS_ACTIVE = 12.0

data class MapPoint(val id: String, val title: String, val latitude: Double, val longitude: Double)

@Composable
fun ProjectMap(points: List<MapPoint>, selectedId: String?, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, userLocation: LatLng? = null,
    contentPadding: PaddingValues = PaddingValues(), interactive: Boolean = true, radiusMeters: Double? = null,
    onLoaded: () -> Unit = {}) {
    val camera = rememberCameraPositionState()
    // Com um projeto selecionado, a pessoa não se afasta muito da visão dos prédios em 3D; a visão geral e o raio de atendimento ficam livres.
    val limitZoomOut = interactive && selectedId != null && radiusMeters == null
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
    LaunchedEffect(points, selectedId, loaded, userLocation, radiusMeters) {
        if (!loaded) return@LaunchedEffect
        val selected = points.firstOrNull { it.id == selectedId }
        when {
            userLocation != null -> camera.animate(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            selected != null && radiusMeters != null -> camera.animate(CameraUpdateFactory.newLatLngBounds(radiusBounds(LatLng(selected.latitude, selected.longitude), radiusMeters), RADIUS_PADDING))
            selected != null -> camera.animate(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(selected.latitude, selected.longitude)).zoom(SELECTED_ZOOM).tilt(SELECTED_TILT).build()))
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
        googleMapOptionsFactory = { GoogleMapOptions().apply { if (BuildConfig.MAP_ID.isNotBlank()) mapId(BuildConfig.MAP_ID) } },
        mapColorScheme = ComposeMapColorScheme.LIGHT,
        properties = MapProperties(minZoomPreference = if (limitZoomOut) MIN_ZOOM_SELECTED else DEFAULT_MIN_ZOOM, maxZoomPreference = MAX_ZOOM,
            isBuildingEnabled = true, isIndoorEnabled = false),
        contentPadding = contentPadding,
        uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false, indoorLevelPickerEnabled = false, compassEnabled = false,
            scrollGesturesEnabled = interactive, zoomGesturesEnabled = interactive, tiltGesturesEnabled = interactive, rotationGesturesEnabled = interactive),
        onMapLoaded = { loaded = true; onLoaded() }) {
        if (loaded) points.forEach { point -> key(point.id) {
            val position = LatLng(point.latitude, point.longitude)
            val active = point.id == selectedId
            if (active && radiusMeters != null) Circle(center = position, radius = radiusMeters,
                fillColor = colors.action.copy(alpha = .1f), strokeColor = colors.action, strokeWidth = 2f, zIndex = 0.5f)
            // O disco fica deitado no chão: acompanha a inclinação do mapa, enquanto o pino permanece de pé e legível.
            Circle(center = position, radius = if (active) GROUND_RADIUS_ACTIVE else GROUND_RADIUS,
                fillColor = colors.action.copy(alpha = if (active) .28f else .16f),
                strokeColor = colors.action, strokeWidth = if (active) 3f else 1.5f, zIndex = 1f)
            Marker(state = rememberUpdatedMarkerState(position),
                icon = markerIcons?.get(active), anchor = Offset(.5f, .96f),
                title = point.title, zIndex = 2f, onClick = { onSelect(point.id); true })
        } }
        userLocation?.let { Circle(center = it, radius = 15.0,
            fillColor = androidx.compose.ui.graphics.Color(0x334285F4), strokeColor = androidx.compose.ui.graphics.Color(0xFF4285F4), strokeWidth = 2f) }
    }
}

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val RADIUS_PADDING = 40

/** Retângulo que contém o círculo de [meters] ao redor de [center]. */
private fun radiusBounds(center: LatLng, meters: Double): LatLngBounds {
    val latitude = Math.toDegrees(meters / EARTH_RADIUS_METERS)
    val longitude = Math.toDegrees(meters / (EARTH_RADIUS_METERS * Math.cos(Math.toRadians(center.latitude))))
    return LatLngBounds(LatLng(center.latitude - latitude, center.longitude - longitude), LatLng(center.latitude + latitude, center.longitude + longitude))
}
