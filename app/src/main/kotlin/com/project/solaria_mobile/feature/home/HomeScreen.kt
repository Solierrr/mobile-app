@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaHeaderGray
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaSize
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import com.project.solaria_mobile.feature.communication.CallsBottomSheet
import com.project.solaria_mobile.presentation.navigation.AppDestination
import com.project.solaria_mobile.feature.home.data.CompanyLogoId
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onOpenPage: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::onCallsQueryChange, onOpenPage, modifier)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCallsQueryChange: (String) -> Unit,
    onOpenPage: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var callsOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SolariaWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SolariaHeaderGray)
                .padding(horizontal = SolariaSpace.screen)
                .padding(top = SolariaSpace.screenTop, bottom = SolariaSpace.xl),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SolariaAvatar(
                    initials = "S",
                    modifier = Modifier.size(SolariaSize.compactHeaderControl).clickable { onOpenPage(AppDestination.ProfileSelf) },
                    size = SolariaSize.compactHeaderControl,
                    color = SolariaOrange,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                    RoundIconButton(
                        icon = LucideR.drawable.lucide_ic_bell,
                        contentDescription = "Notificações",
                        onClick = { onOpenPage(AppDestination.Notifications) },
                        size = SolariaSize.compactHeaderControl,
                        badge = "10",
                    )
                    RoundIconButton(
                        icon = LucideR.drawable.lucide_ic_ellipsis,
                        contentDescription = "Mais opções",
                        onClick = { menuOpen = true },
                        size = SolariaSize.compactHeaderControl,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                SectionHeading(text = "Empresas recentes", faded = true)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
                ) {
                    state.recentCompanies.forEach { company ->
                        Surface(
                            modifier = Modifier.size(SolariaSize.companyLogo),
                            onClick = { onOpenPage(AppDestination.Enterprise) },
                            shape = RoundedCornerShape(SolariaRadius.companyLogo),
                            color = SolariaWhite,
                            shadowElevation = 2.dp,
                        ) {
                            Image(
                                painter = painterResource(when (company.logoId) {
                                    CompanyLogoId.GOOGLE -> R.drawable.company_logo_1
                                    CompanyLogoId.SWIFT -> R.drawable.company_logo_2
                                    CompanyLogoId.AMAZON -> R.drawable.company_logo_3
                                    CompanyLogoId.PARTNER -> R.drawable.company_logo_4
                                    CompanyLogoId.SOLAR -> R.drawable.company_logo_5
                                }),
                                contentDescription = company.name,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                SectionHeading(
                    text = "Projetos recentes",
                    faded = true,
                    action = "Ver todos",
                    onAction = { onOpenPage(AppDestination.Projects) },
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(SolariaSpace.md),
                ) {
                    state.recentProjects.forEach { project ->
                        RecentProjectCard(
                            name = project.name,
                            onClick = { onOpenPage(AppDestination.Project) },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SolariaSpace.screen)
                .padding(top = SolariaSpace.xl, bottom = SolariaSpace.screenBottom),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            SectionHeading(text = "Opções", faded = true)
            ActionRow("Projetos", LucideR.drawable.lucide_ic_folder, { onOpenPage(AppDestination.Projects) })
            ActionRow("Chamados", LucideR.drawable.lucide_ic_phone, { callsOpen = true }, badge = "10")
            ActionRow("Agenda", LucideR.drawable.lucide_ic_calendar_days, { onOpenPage(AppDestination.Schedule) })
            ActionRow("Conversas", LucideR.drawable.lucide_ic_message_circle, { onOpenPage(AppDestination.Chats) }, badge = "10")
            ActionRow("Conexões", LucideR.drawable.lucide_ic_users, { onOpenPage(AppDestination.Connections) })
            ActionRow("Sua empresa", LucideR.drawable.lucide_ic_building, { onOpenPage(AppDestination.Enterprise) })
            ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, { onOpenPage(AppDestination.Settings) })
        }
    }

    if (menuOpen) {
        ModalBottomSheet(
            onDismissRequest = { menuOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SolariaSpace.screen)
                    .padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
                verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
            ) {
                ScreenTitle("Opções", onBack = { menuOpen = false })
                Spacer(Modifier.height(SolariaSpace.xs))
                ActionRow("Listar projetos", LucideR.drawable.lucide_ic_folder, {
                    scope.launch { sheetState.hide(); menuOpen = false; onOpenPage(AppDestination.Projects) }
                }, trailing = "24 projetos")
                ActionRow("Novo projeto", LucideR.drawable.lucide_ic_folder_plus, { menuOpen = false })
                ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, {
                    scope.launch { sheetState.hide(); menuOpen = false; onOpenPage(AppDestination.Settings) }
                })
                Spacer(Modifier.height(SolariaSpace.section))
            }
        }
    }

    if (callsOpen) {
        CallsBottomSheet(
            calls = state.recentCalls,
            query = state.callsQuery,
            onQueryChange = onCallsQueryChange,
            onDismiss = { callsOpen = false },
            onOpenCall = { callsOpen = false; onOpenPage(AppDestination.Call) },
            onOpenAll = { callsOpen = false; onOpenPage(AppDestination.Calls) },
        )
    }
}

@Composable
private fun RecentProjectCard(name: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(SolariaSpace.projectCardWidth).height(SolariaSpace.projectCardHeight),
        onClick = onClick,
        shape = RoundedCornerShape(SolariaRadius.large),
        color = SolariaWhite,
        shadowElevation = 2.dp,
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.project_building),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = SolariaSpace.xs, vertical = SolariaSpace.sm),
                shape = RoundedCornerShape(SolariaRadius.pill),
                color = SolariaWhite,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = name,
                    modifier = Modifier.padding(horizontal = SolariaSpace.sm, vertical = SolariaSpace.xs),
                    color = SolariaInk,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
