package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R

/*
 * Situações, tipos e papéis chegam dos dados já com um rótulo em português, que também serve de chave (filtros e cores
 * comparam com ele). Estas funções traduzem o rótulo só na hora de mostrar; um valor que não conhecem passa como está.
 */

@Composable
fun localizedStatus(label: String): String = when (label) {
    "Aberto" -> stringResource(R.string.status_open)
    "Em andamento" -> stringResource(R.string.status_in_progress)
    "Concluído" -> stringResource(R.string.status_completed)
    "Cancelado" -> stringResource(R.string.status_canceled)
    "Situação indisponível" -> stringResource(R.string.status_unavailable)
    else -> label
}

@Composable
fun localizedActivityType(label: String): String = when (label) {
    "Serviço" -> stringResource(R.string.type_service)
    "Visita" -> stringResource(R.string.type_visit)
    "Reunião" -> stringResource(R.string.type_meeting)
    "Outra" -> stringResource(R.string.type_other)
    else -> label
}

@Composable
fun localizedRole(label: String): String = when (label) {
    "Gerente" -> stringResource(R.string.role_manager)
    "Membro" -> stringResource(R.string.role_member)
    "Observador" -> stringResource(R.string.role_viewer)
    else -> label
}
