@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.project.solaria_mobile.feature.discovery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton
import com.project.solaria_mobile.core.designsystem.components.MapPoint
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ProjectMap
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.project.domain.hasCoordinates
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun MapRoute(onBack: () -> Unit, onOpenProject: (String) -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Mapa dos projetos", onBack) }) { content ->
        MapScreen(content, state as? UiState.Content, onBack, onOpenProject, viewModel::load, viewModel::select, viewModel::resolveLocations)
    }
}

@Composable
private fun MapScreen(state: MapUiState, resource: UiState.Content<*>?, onBack: () -> Unit,
    onOpenProject: (String) -> Unit, onRefresh: () -> Unit, onSelect: (String?) -> Unit, onResolve: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var mapVersion by remember { mutableIntStateOf(0) }
    val locate: () -> Unit = {
        if (!locationBusy) scope.launch {
            locationBusy = true
            val location = currentLocation(context)
            if (location == null) message = "Não foi possível obter sua localização. Confira se o GPS está ligado."
            else { userLocation = LatLng(location.latitude, location.longitude); message = null }
            locationBusy = false
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate() else message = "Você pode continuar consultando os projetos sem permitir sua localização."
    }
    val points = state.projects.filter { it.hasCoordinates() }.map { MapPoint(it.id, it.name, it.latitude!!, it.longitude!!) }
    LaunchedEffect(mapVersion) {
        loaded = false; timedOut = false
        delay(20_000)
        if (!loaded) timedOut = true
    }
    Column(Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        Column(Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.lg)) {
            ScreenTitle("Mapa dos projetos", onBack, actions = {
                RefreshButton(onRefresh, resource?.refreshing == true)
                RoundIconButton(AppIcons.map_pin, if (locationBusy) "Buscando localização" else "Minha localização", {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
                    else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }, enabled = !locationBusy && BuildConfig.MAPS_CONFIGURED)
            })
            resource?.let { ResourceStatus(it) }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted, modifier = Modifier.padding(vertical = Spacing.xs)) }
        }
        if (state.projects.isEmpty()) {
            StateNotice("Nenhum projeto para mostrar", "Os projetos aos quais você tem acesso aparecerão aqui.", icon = AppIcons.map_pin)
        } else {
            if (BuildConfig.MAPS_CONFIGURED) {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    key(mapVersion) { ProjectMap(points, state.selectedId, { userLocation = null; onSelect(it) }, Modifier.fillMaxSize(), userLocation) { loaded = true } }
                    if (!loaded && !timedOut) ContentSkeleton(Modifier.fillMaxSize().background(AppTheme.colors.surface), cards = true)
                    if (timedOut && !loaded) StateNotice("O mapa demorou para carregar", "Confira a conexão e tente novamente.", actions = {
                        PrimaryButton("Tentar novamente", { mapVersion++ }, secondary = true, compact = true)
                    })
                }
            } else StateNotice("Mapa ainda não configurado", "Os endereços continuam disponíveis abaixo.", icon = AppIcons.map_pin)
            Column(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
                if (state.projects.any { !it.hasCoordinates() && it.address.isNotBlank() }) {
                    PrimaryButton(if (state.resolving) "Localizando projetos…" else "Localizar projetos no mapa", onResolve,
                        enabled = !state.resolving && resource?.cached != true, secondary = true, compact = true)
                }
                state.locationError?.let { Text(it.userMessage(), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.danger) }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(state.projects, key = { it.id }) { project ->
                    ActionRow(project.name, AppIcons.map_pin, { userLocation = null; onSelect(project.id) },
                        subtitle = project.address.ifBlank { "Endereço não informado" }, compact = true)
                }
            }
        }
    }
    state.projects.firstOrNull { it.id == state.selectedId }?.let { project ->
        BottomSheet(onDismiss = { onSelect(null) }) {
            Column(Modifier.padding(horizontal = Spacing.screen).padding(bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(project.name, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface)
                if (project.status.isNotBlank()) Text(project.status, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                Text(project.address.ifBlank { "Endereço não informado" }, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurface)
                PrimaryButton("Abrir projeto", { onSelect(null); onOpenProject(project.id) })
                if (project.address.isNotBlank()) PrimaryButton("Como chegar", {
                    val destination = if (project.hasCoordinates()) "${project.latitude},${project.longitude}" else project.address
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    catch (_: android.content.ActivityNotFoundException) { message = "Nenhum aplicativo disponível para abrir o trajeto."; onSelect(null) }
                }, secondary = true)
            }
        }
    }
}
