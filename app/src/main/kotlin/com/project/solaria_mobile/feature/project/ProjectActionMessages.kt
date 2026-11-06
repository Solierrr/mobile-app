package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.userMessage

/**
 * Texto da recusa de uma operação do projeto. A falha não traz a mensagem do servidor, então as recusas conhecidas do
 * api-core (encerrar com pendências, formato ou limite de imagens, permissão) ganham uma explicação própria.
 */
internal fun ProjectAction.Failed.message(resources: android.content.res.Resources): String = when (kind) {
    ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT -> when (operation) {
        ProjectOperation.STATUS -> resources.getString(R.string.pa_sem_conexao_a_situacao_nao)
        ProjectOperation.ARCHIVE -> resources.getString(R.string.pa_sem_conexao_o_projeto_nao)
        ProjectOperation.ACTIVITY_STATUS -> resources.getString(R.string.pa_sem_conexao_a_atividade_nao)
        ProjectOperation.INVITE, ProjectOperation.REVOKE_INVITE, ProjectOperation.RESEND_INVITE, ProjectOperation.RESPOND_INVITE -> resources.getString(R.string.pa_sem_conexao_o_convite_nao)
        ProjectOperation.CREATE_LINK, ProjectOperation.REVOKE_LINK -> resources.getString(R.string.pa_sem_conexao_o_link_nao)
        ProjectOperation.REMOVE_MEMBER -> resources.getString(R.string.pa_sem_conexao_a_pessoa_continua)
        ProjectOperation.SAVE_ACTIVITY -> resources.getString(R.string.pa_sem_conexao_a_atividade_nao_2)
        ProjectOperation.CREATE_REPORT -> resources.getString(R.string.pa_sem_conexao_o_relatorio_nao)
        else -> resources.getString(R.string.pa_sem_conexao_as_imagens_nao)
    }
    ErrorKind.FORBIDDEN -> when (operation) {
        ProjectOperation.STATUS, ProjectOperation.ARCHIVE, ProjectOperation.SET_COVER, ProjectOperation.INVITE,
        ProjectOperation.REVOKE_INVITE, ProjectOperation.RESEND_INVITE, ProjectOperation.REMOVE_MEMBER, ProjectOperation.SAVE_ACTIVITY,
        ProjectOperation.CREATE_LINK, ProjectOperation.REVOKE_LINK,
        -> resources.getString(R.string.pa_so_quem_gerencia_o_projeto)
        ProjectOperation.ACTIVITY_STATUS -> resources.getString(R.string.pa_voce_nao_pode_alterar_esta)
        ProjectOperation.CREATE_REPORT -> resources.getString(R.string.pa_observadores_nao_podem_criar_relatorios)
        ProjectOperation.RESPOND_INVITE -> resources.getString(R.string.pa_este_convite_nao_esta_mais)
        else -> resources.getString(R.string.pa_voce_nao_tem_permissao_para)
    }
    ErrorKind.VALIDATION, ErrorKind.CONFLICT -> when (operation) {
        ProjectOperation.STATUS ->
            if (status?.closes == true) resources.getString(R.string.pa_conclua_ou_cancele_as_atividades)
            else resources.getString(R.string.pa_o_projeto_nao_aceita_essa)
        ProjectOperation.ARCHIVE -> resources.getString(R.string.pa_o_projeto_nao_pode_ser)
        ProjectOperation.ACTIVITY_STATUS -> resources.getString(R.string.pa_a_atividade_nao_aceita_essa)
        ProjectOperation.INVITE -> resources.getString(R.string.pa_confira_o_e_mail_a)
        ProjectOperation.RESPOND_INVITE -> resources.getString(R.string.pa_o_convite_expirou_ou_ja)
        ProjectOperation.CREATE_LINK -> resources.getString(R.string.pa_nao_foi_possivel_criar_o)
        ProjectOperation.REVOKE_LINK -> resources.getString(R.string.pa_este_link_nao_pode_ser)
        ProjectOperation.REMOVE_MEMBER -> resources.getString(R.string.pa_esta_pessoa_nao_pode_ser)
        ProjectOperation.REVOKE_INVITE, ProjectOperation.RESEND_INVITE -> resources.getString(R.string.pa_este_convite_nao_pode_ser)
        ProjectOperation.SAVE_ACTIVITY ->
            resources.getString(R.string.pa_confira_o_titulo_e_os)
        ProjectOperation.UPLOAD_PHOTO -> resources.getString(R.string.pa_use_uma_imagem_jpg_ou)
        ProjectOperation.CREATE_REPORT -> resources.getString(R.string.pa_confira_o_titulo_e_o)
        else -> resources.getString(R.string.pa_a_imagem_nao_pode_ser)
    }
    else -> kind.userMessage(resources)
}

@androidx.compose.runtime.Composable
internal fun ProjectAction.Failed.message(): String = message(androidx.compose.ui.platform.LocalContext.current.resources)
