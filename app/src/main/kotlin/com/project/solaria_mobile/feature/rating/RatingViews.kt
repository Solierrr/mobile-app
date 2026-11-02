package com.project.solaria_mobile.feature.rating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.feature.rating.domain.MAX_RATING_COMMENT
import com.project.solaria_mobile.feature.rating.domain.Rating

private const val STARS = 5
private const val RECENT_COMMENTS = 3

/** Cinco estrelas; [stars] preenche as inteiras. Com [onSelect], cada estrela é um botão. */
@Composable
fun StarRow(stars: Double, modifier: Modifier = Modifier, onSelect: ((Int) -> Unit)? = null) {
    val description = stringResource(R.string.rt_nota_de, "%.1f".format(stars))
    Row(modifier.semantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(STARS) { index ->
            val filled = stars >= index + 0.5
            val size = Modifier.size(if (onSelect != null) 36.dp else 18.dp)
            Icon(
                painterResource(if (filled) AppIcons.star_filled else AppIcons.star), null,
                if (onSelect != null) size.clickable { onSelect(index + 1) } else size,
                tint = if (filled) AppTheme.colors.action else AppTheme.colors.onSurfaceMuted,
            )
        }
    }
}

/** Nota de 1 a 5 e comentário; [initial] preenche a edição de uma avaliação que já existe. */
@Composable
fun RatingSheet(initial: Rating?, onSave: (stars: Int, comment: String) -> Unit, onDismiss: () -> Unit) {
    var stars by remember { mutableIntStateOf(initial?.stars?.toInt() ?: 0) }
    var comment by remember { mutableStateOf(initial?.comment.orEmpty()) }
    BottomSheet(onDismiss = onDismiss) {
        val dismiss = LocalSheetDismiss.current
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(stringResource(if (initial == null) R.string.rt_avaliar else R.string.rt_editar_avaliacao), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            StarRow(stars.toDouble(), onSelect = { stars = it })
            FormField(stringResource(R.string.rt_comentario), comment, { comment = it.take(MAX_RATING_COMMENT) }, singleLine = false, minLines = 3, maxLength = MAX_RATING_COMMENT)
            PrimaryButton(stringResource(R.string.pf_salvar), { dismiss(); onSave(stars, comment) }, enabled = stars > 0)
        }
    }
}

/** Média, avaliação da própria conta e as mais recentes de quem recebeu, no perfil de [evaluatedId]. */
@Composable
fun ProfileRatingsSection(evaluatedId: String, viewModel: ProfileRatingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(false) }
    var confirmRemoval by remember { mutableStateOf(false) }
    LaunchedEffect(evaluatedId) { viewModel.load(evaluatedId) }
    // Sem a lista não há o que mostrar: a seção some em vez de ocupar o perfil com um erro.
    val current = state as? UiState.Content ?: return
    val ui = current.data
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.rt_avaliacoes), faded = true)
        if (ui.average != null) Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            StarRow(ui.average)
            Text(stringResource(R.string.rt_media_total, "%.1f".format(ui.average), ui.ratings.size), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        } else Text(stringResource(R.string.rt_sem_avaliacoes), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        ui.failure?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
        ui.ratings.filter { it.comment.isNotBlank() }.take(RECENT_COMMENTS).forEach { rating ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                StarRow(rating.stars)
                Text(rating.comment, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
        }
        PrimaryButton(stringResource(if (ui.mine == null) R.string.rt_avaliar else R.string.rt_editar_avaliacao), { sheet = true }, enabled = !ui.busy, secondary = true, compact = true)
        if (ui.mine != null) PrimaryButton(stringResource(R.string.rt_excluir_avaliacao), { confirmRemoval = true }, enabled = !ui.busy, secondary = true, compact = true)
    }
    if (sheet) RatingSheet(ui.mine, { stars, comment -> viewModel.save(stars, comment) }, onDismiss = { sheet = false })
    if (confirmRemoval) AppDialog(
        onDismissRequest = { confirmRemoval = false }, title = stringResource(R.string.rt_excluir_pergunta), confirmLabel = stringResource(R.string.ch_excluir),
        dismissLabel = stringResource(R.string.ch_cancelar), destructive = true, onConfirm = { confirmRemoval = false; viewModel.removeMine() },
    )
}

/** As avaliações que a conta fez, com editar e excluir. */
@Composable
fun MyRatingsRoute(onBack: () -> Unit, viewModel: MyRatingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Rating?>(null) }
    var removing by remember { mutableStateOf<Rating?>(null) }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.rt_minhas_avaliacoes), onBack) }, loading = { ListSkeleton() }) { ui ->
        Column(Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ScreenTitle(stringResource(R.string.rt_minhas_avaliacoes), onBack)
            ui.failure?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md), contentPadding = PaddingValues(bottom = Spacing.screenBottom)) {
                if (ui.ratings.isEmpty()) item {
                    Box(Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StateNotice(stringResource(R.string.rt_nenhuma_minha), stringResource(R.string.rt_nenhuma_minha_desc), icon = AppIcons.star)
                    }
                }
                items(ui.ratings, key = { it.id }) { rating ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(rating.evaluatedName ?: stringResource(R.string.gp_participante), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
                        StarRow(rating.stars)
                        if (rating.comment.isNotBlank()) Text(rating.comment, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PrimaryButton(stringResource(R.string.common_edit), { editing = rating }, enabled = !ui.busy, secondary = true, compact = true)
                            PrimaryButton(stringResource(R.string.ch_excluir), { removing = rating }, enabled = !ui.busy, secondary = true, compact = true)
                        }
                    }
                }
            }
        }
    }
    editing?.let { rating -> RatingSheet(rating, { stars, comment -> viewModel.edit(rating.id, stars, comment) }, onDismiss = { editing = null }) }
    removing?.let { rating ->
        AppDialog(
            onDismissRequest = { removing = null }, title = stringResource(R.string.rt_excluir_pergunta), confirmLabel = stringResource(R.string.ch_excluir),
            dismissLabel = stringResource(R.string.ch_cancelar), destructive = true, onConfirm = { removing = null; viewModel.remove(rating.id) },
        )
    }
}
