package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.core.designsystem.components.SkeletonText
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AppButton
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.images.readPickedImage
import com.project.solaria_mobile.core.images.rememberCameraCapture
import com.project.solaria_mobile.feature.project.domain.ProjectReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REPORT_TITLE_MAX = 160
private const val REPORT_CONTENT_MAX = 4000
private const val REPORT_IMAGES_MAX = 5

/**
 * Novo relatório do projeto: título, conteúdo e até cinco imagens da galeria ou da câmera. As imagens também passam a
 * fazer parte das imagens do local, e o relatório aparece na atividade recente do histórico.
 */
@Composable
internal fun ReportForm(viewModel: ProjectSectionsViewModel) {
    val action by viewModel.reportAction.collectAsStateWithLifecycle()
    val dismiss = LocalSheetDismiss.current
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    val images = remember { mutableStateListOf<Pair<ByteArray, String>>() }
    var imageRejected by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.clearReportAction() }
    LaunchedEffect(action) {
        if ((action as? ProjectAction.Done)?.operation == ProjectOperation.CREATE_REPORT) {
            viewModel.clearReportAction()
            dismiss()
        }
    }
    val add: (Uri?) -> Unit = { uri ->
        if (uri != null) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPickedImage(context, uri) }
            imageRejected = picked == null
            if (picked != null && images.size < REPORT_IMAGES_MAX) images += picked
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), add)
    val capture = rememberCameraCapture(add)
    val working = action == ProjectAction.Working

    Text(stringResource(R.string.pd_novo_relatorio), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
    FormField(label = stringResource(R.string.ps_titulo), value = title, onValueChange = { viewModel.clearReportAction(); title = it }, maxLength = REPORT_TITLE_MAX, enabled = !working)
    FormField(
        label = stringResource(R.string.pr_conteudo), value = content, onValueChange = { viewModel.clearReportAction(); content = it },
        singleLine = false, minLines = 4, maxLength = REPORT_CONTENT_MAX, enabled = !working,
    )
    Text(stringResource(R.string.pr_imagens), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    if (images.isNotEmpty()) {
        images.forEachIndexed { index, (bytes, _) ->
            Box(Modifier.fillMaxWidth().height(Sizing.coverHeight).clip(RoundedCornerShape(Radius.large))) {
                RemoteImage(key = "report-image-$index-${bytes.contentHashCode()}", load = { bytes }, description = stringResource(R.string.pr_imagem_n, index + 1), modifier = Modifier.fillMaxWidth().height(Sizing.coverHeight))
                RoundIconButton(
                    AppIcons.trash, stringResource(R.string.pr_remover_imagem_n, index + 1), { images.removeAt(index) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs),
                    background = AppTheme.colors.surface.copy(alpha = 0.9f), enabled = !working,
                )
            }
        }
    }
    if (images.size < REPORT_IMAGES_MAX) {
        ActionRow(stringResource(R.string.ps_escolher_da_galeria), AppIcons.images, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, compact = true, enabled = !working)
        ActionRow(stringResource(R.string.ps_tirar_foto), AppIcons.camera, { capture() }, compact = true, enabled = !working)
    } else {
        Text(stringResource(R.string.pr_limite_imagens, REPORT_IMAGES_MAX), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
    }
    if (imageRejected) Text(stringResource(R.string.pr_use_uma_imagem_jpg_ou), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
    ProjectActionStatus(action, working = stringResource(R.string.pr_enviando_relatorio))
    AppButton(
        stringResource(R.string.pr_enviar_relatorio), { viewModel.createReport(title, content, images.toList()) },
        enabled = title.isNotBlank() && content.isNotBlank(), loading = working,
    )
}

private sealed interface ReportLoad {
    data object Loading : ReportLoad
    data class Ready(val report: ProjectReport) : ReportLoad
    data class Failed(val kind: ErrorKind) : ReportLoad
}

/** Relatório aberto a partir da atividade recente, numa sheet empilhada; tocar numa imagem abre a imagem. */
@Composable
internal fun ReportDetail(reportId: String, viewModel: ProjectSectionsViewModel, onOpenPhoto: (String) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val load by produceState<ReportLoad>(ReportLoad.Loading, reportId, attempt) {
        value = ReportLoad.Loading
        value = when (val result = viewModel.report(reportId)) {
            is ApiResult.Success -> ReportLoad.Ready(result.data)
            is ApiResult.Failure -> ReportLoad.Failed(result.kind)
        }
    }
    when (val state = load) {
        ReportLoad.Loading -> SkeletonGroup {
            SkeletonText(0.7f, height = Spacing.xl)
            SkeletonText(0.45f)
            SkeletonText(0.95f)
            SkeletonText(0.85f)
            Skeleton(Modifier.fillMaxWidth().height(Sizing.coverHeight), RoundedCornerShape(Radius.large))
        }
        is ReportLoad.Failed -> {
            StateNotice(stringResource(R.string.pr_relatorio_indisponivel), state.kind.userMessage(), icon = AppIcons.clock)
            AppButton(stringResource(R.string.pr_tentar_de_novo), { attempt++ })
        }
        is ReportLoad.Ready -> {
            val report = state.report
            Text(report.title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(
                listOf(report.author, report.date).filter { it.isNotBlank() }.joinToString(" · "),
                color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium,
            )
            Text(report.content, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
            report.photoIds.forEachIndexed { index, photoId ->
                Box(Modifier.fillMaxWidth().height(Sizing.coverHeight).clip(RoundedCornerShape(Radius.large)).clickable { onOpenPhoto(photoId) }) {
                    RemoteImage(key = photoId, load = viewModel::photoContent, description = stringResource(R.string.pr_imagem_n, index + 1), modifier = Modifier.fillMaxWidth().height(Sizing.coverHeight))
                }
            }
        }
    }
}
