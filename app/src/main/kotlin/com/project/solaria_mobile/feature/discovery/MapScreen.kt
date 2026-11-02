@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.core.designsystem.components.AppToastHost
import com.project.solaria_mobile.core.designsystem.components.ToastKind
import com.project.solaria_mobile.core.designsystem.components.ToastState
import com.project.solaria_mobile.core.designsystem.components.rememberToastState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import androidx.compose.material3.SheetValue
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
fun MapRoute(onBack: () -> Unit, onOpenProject: (String) -> Unit, viewModel: MapViewModel = hiltViewModel(), geo: GeoAssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val geoSessions by geo.sessions.collectAsStateWithLifecycle()
    val geoThinking by geo.thinking.collectAsStateWithLifecycle()
    val saved by viewModel.savedIds.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    val messageContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel) { viewModel.feedback.collect { launch { toast.show(messageContext.getString(it.text), it.kind) } } }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.ms_mapa_dos_projetos), onBack) }, loading = { MapSkeleton() }) { content ->
        MapScreen(content, state as? UiState.Content, saved, toast, onBack, onOpenProject,
            viewModel::load, viewModel::select, viewModel::resolveLocations, viewModel::toggleSaved,
            GeoAssistant(geo, geoSessions, geoThinking))
    }
}

/** O que a tela do mapa precisa do assistente geográfico: a sessão da visita atual e as mensagens dela. */
internal class GeoAssistant(private val model: GeoAssistantViewModel, private val sessions: List<GeoSession>, private val thinking: Set<String>) {
    val openOnStart: Boolean get() = model.openOnStart
    fun messages(project: ProjectSummary): List<GeoMessage> = model.messages(model.sessionId(project), sessions)
    fun isThinking(project: ProjectSummary): Boolean = model.sessionId(project) in thinking
    fun send(project: ProjectSummary, text: String) = model.send(project, text)
}

@Composable
private fun MapScreen(state: MapUiState, resource: UiState.Content<*>?, saved: Set<String>, toast: ToastState,
    onBack: () -> Unit, onOpenProject: (String) -> Unit, onRefresh: () -> Unit,
    onSelect: (String?) -> Unit, onResolve: () -> Unit, onSave: (String) -> Unit,
    geo: GeoAssistant? = null) {
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
    var assistantFor by remember { mutableStateOf<ProjectSummary?>(null) }
    val resources = context.resources
    val notice: (String, ToastKind) -> Unit = { text, kind -> scope.launch { toast.show(text, kind) } }
    val locate: () -> Unit = {
        if (!locationBusy) scope.launch {
            locationBusy = true
            try {
                val location = currentLocation(context)
                if (location == null) toast.show(resources.getString(R.string.ms_nao_foi_possivel_obter_sua), ToastKind.Error)
                else userLocation = LatLng(location.latitude, location.longitude)
            } finally { locationBusy = false }
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate()
        else notice(resources.getString(R.string.ms_voce_pode_consultar_os_projetos), ToastKind.Info)
    }
    val selected = state.projects.firstOrNull { it.id == state.selectedId }
    val points = state.projects.filter { it.hasCoordinates() }.map { MapPoint(it.id, it.name, it.latitude!!, it.longitude!!) }
    val select: (String) -> Unit = { userLocation = null; onSelect(it); scope.launch { sheetState.expand() } }
    LaunchedEffect(state.selectedId) { if (selected != null) sheetState.expand() }
    LaunchedEffect(mapVersion) { loaded = false; timedOut = false; delay(20_000); if (!loaded) timedOut = true }
    BackHandler(sheetState.targetValue == SheetValue.Expanded) { scope.launch { sheetState.partialExpand() } }
    Box(Modifier.fillMaxSize()) {
    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize().onSizeChanged { screenHeight = it.height },
        scaffoldState = scaffold,
        sheetPeekHeight = Sizing.mapSheetPeek,
        sheetShape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        sheetContainerColor = AppTheme.colors.surface,
        sheetContentColor = AppTheme.colors.onSurface,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 2.dp,
        sheetDragHandle = { MapSheetHandle(sheetState.targetValue == SheetValue.Expanded) {
            scope.launch { if (sheetState.targetValue == SheetValue.Expanded) sheetState.partialExpand() else sheetState.expand() }
        } },
        sheetContent = {
            MapProjectPanel(state, resource, selected, saved, select, onSave,
                onClose = { onSelect(null) }, onOpen = onOpenProject, onAssistant = { assistantFor = it },
                onDirections = { project ->
                    // Um endereço geo: é aberto por qualquer app de mapas ou de rota (Google Maps, Waze…), e a pessoa escolhe.
                    val query = if (project.hasCoordinates()) "${project.latitude},${project.longitude}(${Uri.encode(project.name)})" else Uri.encode(project.address)
                    val route = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                    try { context.startActivity(Intent.createChooser(route, resources.getString(R.string.ms_tracar_rota_com))) }
                    catch (_: android.content.ActivityNotFoundException) { notice(resources.getString(R.string.ms_nenhum_aplicativo_disponivel_para_tracar), ToastKind.Error) }
                },
                onExpand = { scope.launch { sheetState.expand() } },
                expanded = sheetState.targetValue == SheetValue.Expanded,
                onTogglePanel = { scope.launch { if (sheetState.targetValue == SheetValue.Expanded) sheetState.partialExpand() else sheetState.expand() } },
            )
        },
    ) {
        // Aberto a partir de uma sessão da lista: sobe a folha assim que o projeto dela estiver selecionado.
        var openedFromList by remember { mutableStateOf(false) }
        LaunchedEffect(selected?.id) { if (geo?.openOnStart == true && !openedFromList && selected != null) { openedFromList = true; assistantFor = selected } }
        assistantFor?.let { project ->
            geo?.let { g -> GeoAssistantSheet(project, g.messages(project), g.isThinking(project), { g.send(project, it) }, onDismiss = { assistantFor = null }) }
        }
        Box(Modifier.fillMaxSize()) {
            if (BuildConfig.MAPS_CONFIGURED) {
                key(mapVersion) {
                    ProjectMap(points, state.selectedId, select, Modifier.fillMaxSize(), userLocation,
                        PaddingValues(top = Sizing.mapContentTop, bottom = visibleSheet + Spacing.sm)) { loaded = true }
                }
                // Enquanto o mapa carrega, só o fundo do mapa: painel e controles continuam no lugar, sem cartões genéricos por cima.
                if (!loaded && !timedOut) Box(Modifier.fillMaxSize().background(AppTheme.colors.mapPlaceholder))
                if (!loaded && timedOut) Box(Modifier.fillMaxSize().padding(top = Sizing.mapControlsTop, bottom = visibleSheet), contentAlignment = Alignment.Center) {
                    StateNotice(stringResource(R.string.ms_o_mapa_demorou_para_carregar), "Confira a conexão e tente novamente.", actions = {
                        PrimaryButton(stringResource(R.string.common_try_again), { mapVersion++ }, secondary = true, compact = true)
                    })
                }
            } else Box(Modifier.fillMaxSize().padding(top = Sizing.mapControlsTop, bottom = visibleSheet), contentAlignment = Alignment.Center) {
                StateNotice(stringResource(R.string.ms_mapa_ainda_nao_configurado), "Os endereços continuam disponíveis no painel.", icon = AppIcons.map_pin)
            }
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(AppIcons.chevron_left, stringResource(R.string.common_back), onBack, background = AppTheme.colors.surface.copy(alpha = .95f))
                Spacer(Modifier.weight(1f))
                RefreshButton(onRefresh, resource?.refreshing == true, stringResource(R.string.pl_atualizar_projetos), background = AppTheme.colors.surface.copy(alpha = .95f))
            }
            // Projetos sem coordenadas ainda não estão no mapa: o botão procura o endereço de cada um.
            if (state.projects.any { !it.hasCoordinates() && it.address.isNotBlank() }) RoundIconButton(
                AppIcons.map_pin, if (state.resolving) stringResource(R.string.ms_localizando_projetos) else stringResource(R.string.ms_localizar_projetos_no_mapa), onResolve,
                Modifier.align(Alignment.BottomEnd).padding(end = Spacing.md, bottom = visibleSheet + Spacing.sm + Sizing.touchTarget + Spacing.xs),
                background = AppTheme.colors.surface.copy(alpha = .95f),
                enabled = !state.resolving && resource?.cached != true && BuildConfig.MAPS_CONFIGURED,
            )
            RoundIconButton(AppIcons.locate, if (locationBusy) stringResource(R.string.ms_buscando_localizacao) else stringResource(R.string.ms_minha_localizacao), {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
                else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }, Modifier.align(Alignment.BottomEnd).padding(end = Spacing.md, bottom = visibleSheet + Spacing.sm),
                background = AppTheme.colors.surface.copy(alpha = .95f), enabled = !locationBusy && BuildConfig.MAPS_CONFIGURED)
        }
    }
    // Fora do painel: o aviso fica por cima dele e sempre rente ao pé da tela.
    AppToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(Spacing.md))
    }
}
