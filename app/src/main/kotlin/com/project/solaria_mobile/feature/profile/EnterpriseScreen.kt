package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StateNoticePage
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.SkeletonText
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.feature.profile.domain.CompanyProfile
import com.project.solaria_mobile.feature.project.ProjectListCard

private const val COMPANY_TITLE = "Sua empresa"

@Composable
fun EnterpriseRoute(
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: CompanyViewModel = hiltViewModel(),
    images: ImageLoaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val offline = com.project.solaria_mobile.core.offline.LocalOffline.current
    val uriHandler = LocalUriHandler.current
    UiStateHost(state, viewModel::load, onBack = onBack,
        header = { ScreenTitle(COMPANY_TITLE, onBack) },
        loading = { EnterpriseSkeleton() },
        empty = { StateNotice(stringResource(R.string.en_sua_conta_nao_tem_empresa), stringResource(R.string.en_voce_pode_continuar_acessando_seus), icon = AppIcons.building, actions = { PrimaryButton(stringResource(R.string.common_back), onBack, secondary = true, compact = true) }) },
    ) { company ->
        if (!company.detailsAvailable) {
            StateNoticePage(header = { ScreenTitle(COMPANY_TITLE, onBack) }) {
                StateNotice(stringResource(R.string.en_o_perfil_ainda_nao_esta), stringResource(R.string.en_no_momento_nao_ha_informacoes), icon = AppIcons.building,
                    actions = { PrimaryButton(stringResource(R.string.common_back), onBack, secondary = true, compact = true) })
            }
            return@UiStateHost
        }
        val resource = state as? UiState.Content
        val webUrl = companyWebUrl(BuildConfig.WEB_BASE_URL, company.slug)
        Column(Modifier.fillMaxSize().background(AppTheme.colors.surface).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(Sizing.coverHeight)) {
                if (company.bannerUrl != null) {
                    RemoteImage(key = company.bannerUrl, load = images::bytes, description = "Capa de ${company.name}", modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(AppTheme.colors.mapPlaceholder), contentAlignment = Alignment.Center) {
                        Icon(painterResource(AppIcons.building), stringResource(R.string.en_sem_capa_cadastrada), Modifier.size(Sizing.icon * 1.5f), tint = AppTheme.colors.onSurfaceSubtle)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RoundIconButton(AppIcons.chevron_left, stringResource(R.string.common_back), onBack, background = AppTheme.colors.surface.copy(alpha = 0.86f))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        if (webUrl != null) RoundIconButton(AppIcons.globe, stringResource(R.string.en_abrir_a_pagina_da_empresa), { uriHandler.openUri(webUrl) }, background = AppTheme.colors.surface.copy(alpha = 0.86f))
                        RefreshButton(viewModel::load, resource?.refreshing == true, stringResource(R.string.en_atualizar_empresa), background = AppTheme.colors.surface.copy(alpha = 0.86f))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().offset(y = -Spacing.xxl).clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                    .background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.xl, bottom = Spacing.screenBottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                CompanyHeader(company, images)
                if (resource?.refreshError != null && !offline) ResourceStatus(resource)
                SectionHeading(stringResource(R.string.en_projetos_em_que_voce_participa))
                if (company.projects.isEmpty()) {
                    StateNotice(stringResource(R.string.en_nenhum_projeto_por_aqui), stringResource(R.string.en_voce_ainda_nao_participa_de), icon = AppIcons.folder)
                } else {
                    company.projects.forEach { project -> ProjectListCard(project, grid = false, onClick = { onOpenProject(project.id) }) }
                }
            }
        }
    }
}

/** Logo à esquerda e, ao lado, o nome da empresa e, abaixo, o lugar, como o cabeçalho do perfil. */
@Composable
private fun CompanyHeader(company: CompanyProfile, images: ImageLoaderViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            initialsOf(company.name), size = Sizing.avatarLarge,
            // Logos costumam ter fundo transparente: sem moldura cinza e com um respiro, para não colar nas bordas.
            color = if (company.logoUrl != null) Color.Transparent else AppTheme.colors.inputFill,
            shape = RoundedCornerShape(Radius.medium),
            imageUrl = company.logoUrl, loadImage = images::bytes, textColor = AppTheme.colors.onSurface, imageContentScale = ContentScale.Fit,
            contentPadding = Spacing.xs,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(company.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (company.place.isNotBlank()) Text(company.place, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Capa no topo e painel sobreposto com cabeçalho e projetos, como a empresa carregada. */
@Composable
private fun EnterpriseSkeleton() {
    Column(Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        SkeletonGroup(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Skeleton(Modifier.fillMaxWidth().height(Sizing.coverHeight), RoundedCornerShape(0.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SkeletonText(0.3f)
                SkeletonText(0.7f, height = Spacing.xl)
                SkeletonText(0.45f)
                repeat(3) { Skeleton(Modifier.fillMaxWidth().height(Sizing.avatarLarge + Spacing.md), RoundedCornerShape(Radius.large)) }
            }
        }
    }
}
