package com.project.solaria_mobile.presentation.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import com.project.solaria_mobile.core.designsystem.theme.SolariamobileTheme
import com.project.solaria_mobile.presentation.communication.CallsBottomSheet
import kotlinx.coroutines.launch

private data class CompanyLogo(val name: String, val image: Int)

private val recentCompanies = listOf(
    CompanyLogo("Google", R.drawable.company_logo_1),
    CompanyLogo("O Swift", R.drawable.company_logo_2),
    CompanyLogo("Amazon", R.drawable.company_logo_3),
    CompanyLogo("Parceiro", R.drawable.company_logo_4),
    CompanyLogo("Solar", R.drawable.company_logo_5),
)

@Composable
fun HomeScreen(
    onOpenPage: (String) -> Unit,
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
                .background(Color(0xFFE7E7E7))
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
                    modifier = Modifier.size(42.dp).clickable { onOpenPage("profile/self") },
                    size = 42.dp,
                    color = SolariaOrange,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                    RoundIconButton(
                        icon = R.drawable.lucide_ic_bell,
                        contentDescription = "Notificações",
                        onClick = { onOpenPage("notifications") },
                        size = 42.dp,
                    )
                    RoundIconButton(
                        icon = R.drawable.lucide_ic_ellipsis,
                        contentDescription = "Mais opções",
                        onClick = { menuOpen = true },
                        size = 42.dp,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                SectionHeading(text = "Empresas recentes", faded = true)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
                ) {
                    recentCompanies.forEach { company ->
                        Surface(
                            modifier = Modifier.size(60.dp),
                            onClick = { onOpenPage("enterprise") },
                            shape = RoundedCornerShape(18.dp),
                            color = SolariaWhite,
                            shadowElevation = 2.dp,
                        ) {
                            Image(
                                painter = painterResource(company.image),
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
                    onAction = { onOpenPage("projects") },
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(SolariaSpace.md),
                ) {
                    repeat(4) { index ->
                        RecentProjectCard(
                            name = if (index == 0) "Fortech Enterprise" else listOf("Solar Tech", "Residencial Aurora", "Unidade Paraíba")[index - 1],
                            onClick = { onOpenPage("project") },
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
            ActionRow("Projetos", R.drawable.lucide_ic_folder, { onOpenPage("projects") })
            ActionRow("Chamados", R.drawable.lucide_ic_phone, { callsOpen = true }, badge = "10")
            ActionRow("Agenda", R.drawable.lucide_ic_calendar_days, { onOpenPage("schedule") })
            ActionRow("Conversas", R.drawable.lucide_ic_message_circle, { onOpenPage("chats") }, badge = "10")
            ActionRow("Conexões", R.drawable.lucide_ic_users, { onOpenPage("connections") })
            ActionRow("Sua empresa", R.drawable.lucide_ic_building, { onOpenPage("enterprise") })
            ActionRow("Configurações", R.drawable.lucide_ic_settings, { onOpenPage("settings") })
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
                ActionRow("Listar projetos", R.drawable.lucide_ic_folder, {
                    scope.launch { sheetState.hide(); menuOpen = false; onOpenPage("projects") }
                }, trailing = "24 projetos")
                ActionRow("Novo projeto", R.drawable.lucide_ic_folder_plus, { menuOpen = false })
                ActionRow("Configurações", R.drawable.lucide_ic_settings, {
                    scope.launch { sheetState.hide(); menuOpen = false; onOpenPage("settings") }
                })
                Spacer(Modifier.height(SolariaSpace.section))
            }
        }
    }

    if (callsOpen) {
        CallsBottomSheet(
            onDismiss = { callsOpen = false },
            onOpenCall = { callsOpen = false; onOpenPage("call") },
            onOpenAll = { callsOpen = false; onOpenPage("calls") },
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SolariamobileTheme {
        HomeScreen(onOpenPage = {})
    }
}
