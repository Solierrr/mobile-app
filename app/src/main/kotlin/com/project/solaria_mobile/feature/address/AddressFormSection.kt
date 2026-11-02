@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.project.solaria_mobile.feature.address

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.designsystem.components.Appear
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.MapPoint
import com.project.solaria_mobile.core.designsystem.components.ProjectMap
import com.project.solaria_mobile.core.designsystem.components.SelectableChip
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.address.domain.AddressField
import com.project.solaria_mobile.feature.address.domain.AddressFields
import com.project.solaria_mobile.feature.address.domain.ServiceRadius
import com.project.solaria_mobile.feature.address.domain.ZIP_LENGTH

/**
 * Endereço por CEP: ao completar os 8 números, rua, bairro, cidade e UF são preenchidos, o foco vai para o número e o
 * endereço aparece no mapa. Os demais campos só aparecem depois da consulta (ou quando já há endereço), e continuam
 * editáveis, inclusive se o CEP não for encontrado. Serve a qualquer formulário que colete um endereço.
 *
 * Com [onRadiusChange], o formulário também define um raio de atendimento: um botão no canto do mapa alterna entre os
 * raios predefinidos e o círculo aparece no próprio mapa. Ao lado dele, outro botão mostra os raios como opções logo
 * abaixo do mapa; sem mapa (serviço indisponível), as opções aparecem sempre.
 */
@Composable
fun AddressFormSection(
    address: AddressFields,
    onChange: ((AddressFields) -> AddressFields) -> Unit,
    errors: Set<AddressField>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    radiusKm: Int? = null,
    onRadiusChange: ((Int?) -> Unit)? = null,
    viewModel: AddressLookupViewModel = hiltViewModel(),
) {
    val lookup by viewModel.state.collectAsStateWithLifecycle()
    val numberFocus = remember { FocusRequester() }
    val showDetails = lookup.searched || address.street.isNotBlank() || address.city.isNotBlank()
    // Já existe endereço (edição): posiciona no mapa sem esperar uma digitação.
    LaunchedEffect(address.geocodeQuery()) { viewModel.locate(address.geocodeQuery()) }
    // Depois que a consulta preenche a rua, a próxima coisa a digitar é o número.
    LaunchedEffect(lookup.searched, address.street) {
        if (lookup.searched && address.street.isNotBlank() && address.number.isBlank()) runCatching { numberFocus.requestFocus() }
    }
    val mapPoint = lookup.point?.takeIf { BuildConfig.MAPS_CONFIGURED }
    var optionsShown by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        FormField(
            label = stringResource(R.string.address_zip_label),
            value = address.zipDigits,
            onValueChange = { raw ->
                val zip = raw.filter(Char::isDigit).take(ZIP_LENGTH)
                onChange { it.copy(zip = zip) }
                viewModel.onZipChanged(zip, address.copy(zip = zip), onChange)
            },
            visualTransformation = ZipVisualTransformation,
            error = if (AddressField.ZIP in errors) stringResource(R.string.address_zip_error, ZIP_LENGTH) else null,
            keyboardType = KeyboardType.Number,
            maxLength = ZIP_LENGTH,
            enabled = enabled,
        )
        zipHint(lookup.zip, showDetails)?.let { hint ->
            Text(hint.first, color = if (hint.second) AppTheme.colors.danger else AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (showDetails) Appear { Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            mapPoint?.let { point ->
                Appear { Box(Modifier.fillMaxWidth().height(MAP_HEIGHT).clip(RoundedCornerShape(Radius.large))) {
                    ProjectMap(
                        points = listOf(MapPoint("address", address.oneLine(), point.latitude, point.longitude)),
                        selectedId = "address", onSelect = {}, modifier = Modifier.fillMaxWidth().height(MAP_HEIGHT), interactive = false,
                        radiusMeters = radiusKm?.let { it * METERS_PER_KM },
                    )
                    if (onRadiusChange != null) Row(Modifier.align(Alignment.BottomEnd).padding(Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        MapGlassButton(
                            AppIcons.sliders_horizontal, stringResource(R.string.address_radius_options),
                            { optionsShown = !optionsShown },
                            stateText = stringResource(if (optionsShown) R.string.address_options_shown else R.string.address_options_hidden),
                        )
                        RadiusButton(radiusKm, { onRadiusChange(ServiceRadius.next(radiusKm)) })
                    }
                } }
            }
            if (onRadiusChange != null && (mapPoint == null || optionsShown)) Appear { RadiusOptions(radiusKm, onRadiusChange) }
            Text(stringResource(R.string.address_section_title), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
            FormField(
                label = stringResource(R.string.address_street), value = address.street, onValueChange = { v -> onChange { it.copy(street = v) } },
                error = if (AddressField.STREET in errors) stringResource(R.string.address_street_error) else null, maxLength = 160, enabled = enabled,
            )
            FormField(
                label = stringResource(R.string.address_number), value = address.number, onValueChange = { v -> onChange { it.copy(number = v) } },
                error = if (AddressField.NUMBER in errors) stringResource(R.string.address_number_error) else null,
                maxLength = 20, enabled = enabled, focusRequester = numberFocus,
            )
            FormField(
                label = stringResource(R.string.address_complement), value = address.complement, onValueChange = { v -> onChange { it.copy(complement = v) } },
                maxLength = 120, enabled = enabled,
            )
            FormField(
                label = stringResource(R.string.address_neighborhood), value = address.neighborhood, onValueChange = { v -> onChange { it.copy(neighborhood = v) } },
                error = if (AddressField.NEIGHBORHOOD in errors) stringResource(R.string.address_neighborhood_error) else null, maxLength = 120, enabled = enabled,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                FormField(
                    label = stringResource(R.string.address_city), value = address.city, onValueChange = { v -> onChange { it.copy(city = v) } },
                    modifier = Modifier.weight(0.65f), error = if (AddressField.CITY in errors) stringResource(R.string.address_city_error) else null,
                    maxLength = 120, enabled = enabled,
                )
                FormField(
                    label = stringResource(R.string.address_state), value = address.state,
                    onValueChange = { v -> onChange { it.copy(state = v.uppercase().filter(Char::isLetter)) } },
                    modifier = Modifier.weight(0.35f), error = if (AddressField.STATE in errors) stringResource(R.string.address_state_error) else null,
                    maxLength = 2, imeAction = ImeAction.Done, enabled = enabled,
                )
            }
        } }
    }
}

/**
 * Botão do canto do mapa, em vidro como a barra de navegação: cada toque passa para o próximo raio. Só o ícone aparece
 * até a primeira vez que é tocado; depois ele ganha o raio escolhido ao lado. Sobre o mapa não há desfoque de verdade
 * (ele é uma superfície própria), então o vidro é branco translúcido com borda e sombra suaves.
 */
@Composable
private fun RadiusButton(radiusKm: Int?, onClick: () -> Unit) {
    var touched by rememberSaveable { mutableStateOf(false) }
    MapGlassButton(
        AppIcons.radius, stringResource(R.string.address_radius_title),
        { touched = true; onClick() },
        stateText = radiusKm?.let { "$it km" } ?: stringResource(R.string.address_radius_unset),
        label = radiusKm?.takeIf { touched }?.let { "$it km" },
    )
}

/**
 * Botão de vidro sobre o mapa. Sem sombra de elevação: ela apareceria através do véu translúcido, como uma forma
 * geométrica atrás do ícone; o contorno fino dá o destaque.
 */
@Composable
private fun MapGlassButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit, stateText: String, label: String? = null) {
    val toggleDescription = stringResource(R.string.address_toggle_description, description, stateText)
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = toggleDescription },
        shape = RoundedCornerShape(Radius.pill),
        color = Color.White.copy(alpha = GLASS_ALPHA),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = GLASS_BORDER_ALPHA)),
    ) {
        Row(
            Modifier.height(Sizing.touchTarget).widthIn(min = Sizing.touchTarget).padding(horizontal = if (label == null) 0.dp else Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), null, Modifier.size(Sizing.iconSmall), tint = AppTheme.colors.onSurface)
            label?.let { Text(it, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall) }
        }
    }
}

/** Sem mapa para mostrar o círculo, os mesmos raios aparecem como opções; tocar no escolhido o remove. */
@Composable
private fun RadiusOptions(radiusKm: Int?, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.address_radius_title), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ServiceRadius.PRESETS.forEach { km -> SelectableChip("$km km", radiusKm == km, { onChange(km.takeIf { radiusKm != km }) }) }
        }
    }
}

/** Mensagem sob o CEP e se ela é um erro; some quando a consulta termina bem. */
@Composable
private fun zipHint(status: ZipStatus, detailsShown: Boolean): Pair<String, Boolean>? = when (status) {
    ZipStatus.SEARCHING -> stringResource(R.string.address_zip_searching) to false
    ZipStatus.NOT_FOUND -> stringResource(R.string.address_zip_not_found) to true
    ZipStatus.FAILED -> stringResource(R.string.address_zip_failed) to true
    ZipStatus.IDLE -> if (detailsShown) null else stringResource(R.string.address_zip_idle) to false
}

private val MAP_HEIGHT = 180.dp
private const val GLASS_ALPHA = 0.85f
private const val GLASS_BORDER_ALPHA = 0.08f
private const val METERS_PER_KM = 1000.0
