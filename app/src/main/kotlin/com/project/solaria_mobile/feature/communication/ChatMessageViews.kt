package com.project.solaria_mobile.feature.communication

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.MapPoint
import com.project.solaria_mobile.core.designsystem.components.ProjectMap
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.feature.communication.domain.ChatMessage
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BUBBLE_MAX_WIDTH = 300.dp
private val MEDIA_WIDTH = 240.dp
private val MEDIA_HEIGHT = 180.dp
private val BUBBLE_ROUND = 18.dp
private val SENDER_TOP_PADDING = 10.dp
private val SENDER_GAP = 2.dp
private val SENDER_GAP_ATTACHMENT = 6.dp
private val BUBBLE_JOINT = 6.dp
private val BUBBLE_TAIL = 4.dp

/** "Hoje", "Ontem", o dia da semana para a última semana e a data por extenso para o resto. */
@Composable
fun DaySeparator(date: LocalDate) {
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }
    val label = when {
        date == today -> stringResource(R.string.ch_hoje)
        date == today.minusDays(1) -> stringResource(R.string.ch_ontem)
        date.isAfter(today.minusDays(7)) -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    PillSeparator(label)
}

/** Divisor de novas mensagens, no mesmo formato do separador de dia. */
@Composable
fun NewMessagesDivider() {
    PillSeparator(stringResource(R.string.ch_novas_mensagens))
}

@Composable
private fun PillSeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(Radius.pill), color = AppTheme.colors.inputFill) {
            Text(label, Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xxs), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Balão de quem está digitando: três pontos que pulsam em sequência. */
@Composable
fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    Surface(shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp), color = AppTheme.colors.inputFill) {
        Row(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(3) { index ->
                val alpha = transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse, StartOffset(index * 160)), label = "dot-$index")
                Box(Modifier.size(7.dp).alpha(alpha.value).background(AppTheme.colors.onSurfaceMuted, CircleShape))
            }
        }
    }
}

/**
 * Uma mensagem do histórico: o balão com o conteúdo (texto com markdown, imagem, arquivo ou localização) e, na última de
 * cada sequência, o horário. Em grupo, o nome de quem enviou é a primeira linha do primeiro balão da sequência; as mensagens
 * seguintes da mesma pessoa se emendam ao balão de cima, com o canto reto, e não repetem o nome.
 */
@Composable
fun MessageRow(
    entry: ChatListItem.Entry,
    group: Boolean,
    time: String,
    bubbleModifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
) {
    val message = entry.message
    val mine = message.mine
    val sender = message.senderName?.takeIf { entry.showSender && it.isNotBlank() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Box {
                Surface(
                    modifier = bubbleModifier.widthIn(max = BUBBLE_MAX_WIDTH),
                    color = if (mine) AppTheme.colors.action else AppTheme.colors.inputFill,
                    shape = bubbleShape(mine, entry.firstInGroup, entry.lastInGroup),
                ) {
                    MessageContent(message, sender, mine)
                }
                menu()
            }
            if (entry.lastInGroup) MessageMeta(time)
        }
    }
}

/**
 * Cantos do balão numa sequência da mesma pessoa: o lado em que ela fala (esquerda para os outros, direita para as próprias)
 * fica reto onde o balão encosta no vizinho e com a ponta só no último; o outro lado é sempre arredondado.
 */
private fun bubbleShape(mine: Boolean, first: Boolean, last: Boolean): RoundedCornerShape {
    val top = if (first) BUBBLE_ROUND else BUBBLE_JOINT
    val bottom = if (last) BUBBLE_TAIL else BUBBLE_JOINT
    return if (mine) RoundedCornerShape(topStart = BUBBLE_ROUND, topEnd = top, bottomEnd = bottom, bottomStart = BUBBLE_ROUND)
    else RoundedCornerShape(topStart = top, topEnd = BUBBLE_ROUND, bottomEnd = BUBBLE_ROUND, bottomStart = bottom)
}

/** Só o horário e, se a mensagem foi editada, a marca de editada; o app não mostra visualização nem confirmação de leitura. */
@Composable
private fun MessageMeta(time: String) {
    Text(time, Modifier.padding(top = 3.dp), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
}

/** O que a bolha precisa para baixar mídia, sem passar parâmetros por cada camada da lista. */
class MediaUi(
    val loading: Set<String> = emptySet(),
    val unavailable: Set<String> = emptySet(),
    val onDownload: (mediaId: String, name: String?, expiresAt: java.time.Instant?) -> Unit = { _, _, _ -> },
)

val LocalMediaUi = androidx.compose.runtime.staticCompositionLocalOf { MediaUi() }

/** O anexo, quando há, acima do texto da mensagem. */
@Composable
private fun MessageContent(message: ChatMessage, sender: String? = null, mine: Boolean = false) {
    val attachment = message.attachment
    // Uma localização é enviada sozinha, sem texto no mesmo balão.
    val showText = message.text.isNotBlank() && attachment !is MessageAttachment.Location
    // Com o nome no topo, o balão perde um pouco do respiro de cima e o nome fica quase colado ao texto.
    Column(Modifier.padding(start = Spacing.sm, end = Spacing.sm, bottom = Spacing.sm, top = if (sender != null) SENDER_TOP_PADDING else Spacing.sm)) {
        if (sender != null) Text(sender, Modifier.padding(horizontal = Spacing.xs).padding(bottom = if (attachment != null) SENDER_GAP_ATTACHMENT else SENDER_GAP), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelLarge)
        when (attachment) {
            is MessageAttachment.Image -> ChatImage(attachment)
            is MessageAttachment.File -> FileCard(attachment)
            is MessageAttachment.Location -> LocationCard(attachment)
            null -> Unit
        }
        if (showText) MessageText(message.text, Modifier.padding(horizontal = Spacing.xs).padding(top = if (attachment != null) Spacing.xs else 0.dp), mine)
    }
}

/** Sinais de markdown ou de link; sem eles o texto simples basta e evita o custo do parser em cada balão. */
private val MARKDOWN_HINT = Regex("""[*_~`#>\[\]]|(^|\n)\s*[-+]\s|\d+\.\s|https?://|\n\n""")

@Composable
fun MessageText(text: String, modifier: Modifier = Modifier, mine: Boolean = false) {
    // As próprias mensagens são laranja, com texto branco; os blocos de código ganham um véu claro em vez do fundo da página.
    val color = if (mine) AppTheme.colors.onAccent else AppTheme.colors.onSurface
    val codeFill = if (mine) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f) else AppTheme.colors.surface
    val body = MaterialTheme.typography.bodyMedium
    if (!MARKDOWN_HINT.containsMatchIn(text)) {
        Text(text, modifier, color = color, style = body)
        return
    }
    Markdown(
        content = text,
        modifier = modifier,
        colors = markdownColor(text = color, codeBackground = codeFill, inlineCodeBackground = codeFill, dividerColor = if (mine) color.copy(alpha = 0.6f) else AppTheme.colors.onSurfaceMuted),
        padding = markdownPadding(block = Spacing.xs),
        typography = markdownTypography(
            h1 = body.copy(color = color), h2 = body.copy(color = color), h3 = body.copy(color = color),
            h4 = body.copy(color = color), h5 = body.copy(color = color), h6 = body.copy(color = color),
            text = body, paragraph = body, ordered = body, bullet = body, list = body,
        ),
    )
}

/** Foto enviada: carrega pelo mesmo cache das outras imagens do app; arquivos locais (fotos recém-enviadas) são lidos do aparelho. */
@Composable
private fun ChatImage(image: MessageAttachment.Image, loader: ImageLoaderViewModel = hiltViewModel()) {
    val url = image.url
    val height = if ((image.width ?: 0) > 0 && (image.height ?: 0) > 0)
        (MEDIA_WIDTH * (image.height!!.toFloat() / image.width!!)).coerceIn(80.dp, MEDIA_WIDTH * 2) else MEDIA_HEIGHT
    // Ainda só no servidor: o espaço da foto mostra o andamento, o aviso de indisponível ou o convite a baixar.
    if (url.isBlank()) {
        MediaPlaceholder(image.mediaId, null, image.expiresAt, Modifier.size(MEDIA_WIDTH, height))
        return
    }
    RemoteImage(
        key = url,
        load = { key -> if (key.startsWith("file:")) withContext(Dispatchers.IO) { runCatching { File(Uri.parse(key).path.orEmpty()).readBytes() }.getOrNull() } else loader.bytes(key) },
        description = stringResource(R.string.ch_imagem_enviada),
        modifier = Modifier.size(MEDIA_WIDTH, height).clip(RoundedCornerShape(Radius.medium)),
        contentScale = ContentScale.Crop,
    )
}

/** Mídia que ainda não está no aparelho: baixa ao tocar (as fotos baixam sozinhas), ou diz que o servidor não a guarda mais. */
@Composable
private fun MediaPlaceholder(mediaId: String?, name: String?, expiresAt: java.time.Instant?, modifier: Modifier) {
    val media = LocalMediaUi.current
    val unavailable = mediaId == null || mediaId in media.unavailable
    val loading = mediaId != null && mediaId in media.loading
    Box(
        modifier.clip(RoundedCornerShape(Radius.medium)).background(AppTheme.colors.surface)
            .clickable(enabled = !unavailable && !loading) { mediaId?.let { media.onDownload(it, name, expiresAt) } },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) com.project.solaria_mobile.core.designsystem.components.InlineLoader()
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(painterResource(if (unavailable) AppIcons.shield_alert else AppIcons.download), null, Modifier.size(28.dp), tint = AppTheme.colors.onSurfaceMuted)
            Text(stringResource(if (unavailable) R.string.ch_midia_indisponivel else R.string.ch_toque_para_baixar), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun FileCard(file: MessageAttachment.File) {
    if (file.mimeType.startsWith("audio/") && file.url.isNotBlank()) {
        AudioFileCard(file)
        return
    }
    val context = LocalContext.current
    val media = LocalMediaUi.current
    val size = android.text.format.Formatter.formatShortFileSize(context, file.sizeBytes)
    val description = stringResource(R.string.ch_arquivo_desc, file.name, size)
    val remoteOnly = file.url.isBlank()
    val unavailable = file.mediaId == null || file.mediaId in media.unavailable
    val loading = file.mediaId != null && file.mediaId in media.loading
    Row(
        Modifier.width(MEDIA_WIDTH)
            .clip(RoundedCornerShape(Radius.medium))
            .background(AppTheme.colors.surface)
            .clickable(enabled = !(remoteOnly && (unavailable || loading))) {
                if (remoteOnly) file.mediaId?.let { media.onDownload(it, file.name, file.expiresAt) } else openDocument(context, file)
            }
            .padding(Spacing.sm)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(painterResource(AppIcons.folder), null, Modifier.size(28.dp), tint = AppTheme.colors.action)
        Column(Modifier.weight(1f)) {
            Text(file.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    !remoteOnly -> size
                    unavailable -> stringResource(R.string.ch_midia_indisponivel)
                    loading -> stringResource(R.string.ch_baixando)
                    else -> stringResource(R.string.ch_toque_para_baixar_tamanho, size)
                },
                color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun openDocument(context: android.content.Context, file: MessageAttachment.File) {
    if (!file.url.startsWith("content:") && !file.url.startsWith("file:")) return
    // O arquivo baixado fica na pasta do app: outro aplicativo só o abre por um URI do FileProvider, nunca por `file://`.
    val uri = Uri.parse(file.url).let { parsed ->
        if (parsed.scheme == "file") androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", File(parsed.path.orEmpty())) else parsed
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, file.mimeType.ifBlank { "*/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Mapa estático da posição (o mesmo preview do cadastro de endereço); o toque abre o aplicativo de mapas. Sem chave do Maps, vira um cartão com as coordenadas. */
@Composable
private fun LocationCard(location: MessageAttachment.Location) {
    val context = LocalContext.current
    val coordinates = "%.5f, %.5f".format(location.latitude, location.longitude)
    val title = location.label.ifBlank { stringResource(R.string.ch_localizacao_compartilhada) }
    val open = stringResource(R.string.ch_abrir_no_mapa)
    Column(
        Modifier.width(MEDIA_WIDTH).clip(RoundedCornerShape(Radius.medium)).background(AppTheme.colors.surface)
            .clickable(onClickLabel = open) { openInMaps(context, location) },
    ) {
        Box(Modifier.fillMaxWidth().height(MEDIA_HEIGHT * 0.75f)) {
            if (BuildConfig.MAPS_CONFIGURED) {
                ProjectMap(
                    points = listOf(MapPoint("location", title, location.latitude, location.longitude)),
                    selectedId = "location", onSelect = {}, modifier = Modifier.matchParentSize(), interactive = false,
                )
                // Cobre o mapa para o toque abrir o aplicativo de mapas em vez de mexer na câmera.
                Box(Modifier.matchParentSize().clickable(onClickLabel = open) { openInMaps(context, location) })
            } else {
                Column(
                    Modifier.matchParentSize().background(AppTheme.colors.inputFill).padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(painterResource(AppIcons.map_pin), null, Modifier.size(28.dp), tint = AppTheme.colors.action)
                    Text(stringResource(R.string.ch_mapa_indisponivel), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Column(Modifier.padding(Spacing.sm)) {
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(coordinates, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun openInMaps(context: android.content.Context, location: MessageAttachment.Location) {
    val label = Uri.encode(location.label.ifBlank { "%.5f, %.5f".format(location.latitude, location.longitude) })
    val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}($label)")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Balão de uma mensagem excluída, só com o contorno e o aviso em itálico. */
@Composable
fun DeletedBubble(mine: Boolean, text: String, time: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Surface(
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.inputFill),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(text, Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic))
        }
        Text(time, Modifier.padding(top = 3.dp), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
    }
}
