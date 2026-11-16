@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.project.solaria_mobile.feature.discovery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.MapPoint
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ProjectMap
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.project.domain.hasCoordinates
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.project.solaria_mobile.core.designsystem.components.MapSkeleton

@Composable
fun MapRoute(onBack: () -> Unit, onOpenProject: (String) -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.savedIds.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.feedback.collect { snackbar.showSnackbar(it) } }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Mapa dos projetos", onBack) }, loading = { MapSkeleton() }) { content ->
        MapScreen(content, state as? UiState.Content, saved, snackbar, onBack, onOpenProject,
            viewModel::load, viewModel::select, viewModel::resolveLocations, viewModel::toggleSaved)
    }
}

@Composable
private fun MapScreen(state: MapUiState, resource: UiState.Content<*>?, saved: Set<String>, snackbar: SnackbarHostState,
    onBack: () -> Unit, onOpenProject: (String) -> Unit, onRefresh: () -> Unit,
    onSelect: (String?) -> Unit, onResolve: () -> Unit, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffold = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    var screenHeight by remember { mutableIntStateOf(0) }
    val visibleSheet by remember { derivedStateOf {
        with(density) { (screenHeight - runCatching { sheetState.requireOffset() }.getOrDefault(screenHeight.toFloat())).coerceAtLeast(0f).toDp() }
    } }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationBusy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var mapVersion by remember { mutableIntStateOf(0) }
    val notice: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    val locate: () -> Unit = {
        if (!locationBusy) scope.launch {
            locationBusy = true
            try {
                val location = currentLocation(context)
                if (location == null) snackbar.showSnackbar("Não foi possível obter sua localização. Confira se o GPS está ligado.")
                else userLocation = LatLng(location.latitude, location.longitude)
            } finally { locationBusy = false }
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate()
        else notice("Você pode consultar os projetos sem permitir sua localização.")
    }
    val selected = state.projects.firstOrNull { it.id == state.selectedId }
    val points = state.projects.filter { it.hasCoordinates() }.map { MapPoint(it.id, it.name, it.latitude!!, it.longitude!!) }
    val select: (String) -> Unit = { userLocation = null; onSelect(it); scope.launch { sheetState.expand() } }
    LaunchedEffect(state.selectedId) { if (selected != null) sheetState.expand() }
    LaunchedEffect(mapVersion) { loaded = false; timedOut = false; delay(20_000); if (!loaded) timedOut = true }
    BackHandler(sheetState.targetValue == SheetValue.Expanded) { scope.launch { sheetState.partialExpand() } }
    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize().onSizeChanged { screenHeight = it.height },
        scaffoldState = scaffold,
        sheetPeekHeight = 104.dp,
        sheetShape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        sheetContainerColor = AppTheme.colors.surface,
        sheetContentColor = AppTheme.colors.onSurface,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 2.dp,
        sheetDragHandle = { MapSheetHandle(sheetState.targetValue == SheetValue.Expanded) {
            scope.launch { if (sheetState.targetValue == SheetValue.Expanded) sheetState.partialExpand() else sheetState.expand() }
        } },
        sheetContent = {
            MapProjectPanel(state, resource, selected, saved, select, onSave, onResolve,
                onClose = { onSelect(null) }, onOpen = onOpenProject,
                onDirections = { project ->
                    val destination = if (project.hasCoordinates()) "${project.latitude},${project.longitude}" else project.address
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    catch (_: android.content.ActivityNotFoundException) { notice("Nenhum aplicativo disponível para abrir o trajeto.") }
                },
                onExpand = { scope.launch { sheetState.expand() } },
                expanded = sheetState.targetValue == SheetValue.Expanded,
                onTogglePanel = { scope.launch { if (sheetState.targetValue == SheetValue.Expanded) sheetState.partialExpand() else sheetState.expand() } },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (BuildConfig.MAPS_CONFIGURED) {
                key(mapVersion) {
                    ProjectMap(points, state.selectedId, select, Modifier.fillMaxSize(), userLocation,
                        PaddingValues(top = 88.dp, bottom = visibleSheet + Spacing.sm)) { loaded = true }
                }
                // Enquanto o mapa carrega, só o fundo do mapa: painel e controles continuam no lugar, sem cartões genéricos por cima.
                if (!loaded && !timedOut) Box(Modifier.fillMaxSize().background(AppTheme.colors.mapPlaceholder))
                if (!loaded && timedOut) Box(Modifier.padding(top = 96.dp, bottom = visibleSheet)) {
                    StateNotice("O mapa demorou para carregar", "Confira a conexão e tente novamente.", actions = {
                        PrimaryButton("Tentar novamente", { mapVersion++ }, secondary = true, compact = true)
                    })
                }
            } else Box(Modifier.padding(top = 96.dp, bottom = visibleSheet)) {
                StateNotice("Mapa ainda não configurado", "Os endereços continuam disponíveis no painel.", icon = AppIcons.map_pin)
            }
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(AppIcons.chevron_left, "Voltar", onBack, background = AppTheme.colors.surface.copy(alpha = .95f))
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(Radius.pill), color = AppTheme.colors.surface.copy(alpha = .95f)) {
                    Row(Modifier.height(Sizing.touchTarget).padding(horizontal = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(AppIcons.map_pin), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
                        Text("Seus projetos", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    }
                }
                RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar projetos")
            }
            RoundIconButton(AppIcons.map_pin, if (locationBusy) "Buscando localização" else "Minha localização", {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
                else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }, Modifier.align(Alignment.BottomEnd).padding(end = Spacing.md, bottom = visibleSheet + Spacing.sm),
                background = AppTheme.colors.surface.copy(alpha = .95f), enabled = !locationBusy && BuildConfig.MAPS_CONFIGURED)
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = Spacing.md).padding(bottom = visibleSheet + 72.dp)) { data ->
                Snackbar(data, containerColor = AppTheme.colors.surface, contentColor = AppTheme.colors.onSurface,
                    shape = RoundedCornerShape(Radius.medium))
            }
        }
    }
}
